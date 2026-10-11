package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TreeMap

data class ScannedAudioResult(
    val songs: List<Song>,
    val folders: Map<String, List<Song>>
)

object AudioScanner {
    private const val TAG = "AudioScanner"

    var cachedResult: ScannedAudioResult? = null
        private set

    val cachedFolders: Map<String, List<Song>>
        get() = cachedResult?.folders ?: emptyMap()

    private val songProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATE_ADDED
    )

    private val folderProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATE_ADDED
    )

    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg")

    private const val AUDIO_SELECTION_QUERY = "((${MediaStore.Audio.Media.IS_MUSIC} != 0) OR " +
            "(${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.mp3') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.m4a') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.wav') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.flac') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.aac') OR " +
            "(${MediaStore.Audio.Media.DATA} LIKE '%.ogg') OR " +
            "(${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.mp3'))"

    suspend fun scanMediaAutomatically(context: Context): ScannedAudioResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // Pipeline A: MediaStore query for all audio files (including downloaded, transferred MP3s)
        val mediaStoreSongs = mutableListOf<Song>()
        try {
            contentResolver.query(
                collection,
                songProjection,
                AUDIO_SELECTION_QUERY,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                mediaStoreSongs.addAll(parseSongsCursor(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in MediaStore scan: ${e.message}", e)
        }

        // Pipeline B: Direct file system discovery across storage directories to catch any unindexed .mp3 files
        val directFiles = scanStorageFilesDirectly(context)

        // Merge both pipelines seamlessly
        val combined = mutableListOf<Song>()
        combined.addAll(mediaStoreSongs)
        val existingPaths = mediaStoreSongs.mapNotNull { it.path.takeIf { p -> p.isNotBlank() }?.lowercase() }.toSet()
        for (directSong in directFiles) {
            if (directSong.path.lowercase() !in existingPaths) {
                combined.add(directSong)
            }
        }

        val finalSongs = deduplicateSongs(combined)

        // Pipeline C: Automatic Folder Tree categorization
        val folderMap = TreeMap<String, MutableList<Song>>(String.CASE_INSENSITIVE_ORDER)
        for (song in finalSongs) {
            val folder = song.folder.ifBlank { "Music" }
            folderMap.getOrPut(folder) { mutableListOf() }.add(song)
        }
        val autoOrganizedFolders = folderMap.mapValues { (_, tracks) ->
            tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }

        val result = ScannedAudioResult(songs = finalSongs, folders = autoOrganizedFolders)
        cachedResult = result
        result
    }

    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val result = scanMediaAutomatically(context)
        result.songs
    }

    suspend fun scanDeviceAudioAfter(context: Context, minDateAdded: Long): List<Song> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val songs = mutableListOf<Song>()
        try {
            val selection = "$AUDIO_SELECTION_QUERY AND (${MediaStore.Audio.Media.DATE_ADDED} > ?)"
            contentResolver.query(
                collection,
                songProjection,
                selection,
                arrayOf(minDateAdded.toString()),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                songs.addAll(parseSongsCursor(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanDeviceAudioAfter: ${e.message}", e)
        }
        // Fast lightweight check in standard download/music folders for newly placed files
        val quickNew = scanQuickNewFiles(context, minDateAdded)
        val merged = mutableListOf<Song>()
        merged.addAll(songs)
        val existing = songs.map { it.path.lowercase() }.toSet()
        for (d in quickNew) {
            if (d.path.lowercase() !in existing) {
                merged.add(d)
            }
        }
        deduplicateSongs(merged)
    }

    /**
     * Fast incremental check for newly downloaded audio files without deep recursive scanning.
     */
    fun scanQuickNewFiles(context: Context, minDateAdded: Long): List<Song> {
        val targetDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Music")
        ).filter { it.exists() && it.isDirectory }.distinctBy { it.absolutePath }

        val newFiles = mutableListOf<File>()
        for (dir in targetDirs) {
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.isFile) {
                    val ext = f.extension.lowercase()
                    if (ext in AUDIO_EXTENSIONS && f.length() > 1024) {
                        val fileDate = f.lastModified() / 1000L
                        if (fileDate > minDateAdded) {
                            newFiles.add(f)
                        }
                    }
                }
            }
        }

        if (newFiles.isEmpty()) return emptyList()

        try {
            val paths = newFiles.map { it.absolutePath }.toTypedArray()
            MediaScannerConnection.scanFile(context, paths, null, null)
        } catch (_: Exception) {}

        val result = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()
        for (file in newFiles) {
            try {
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var durationMs = 0L
                var genre: String? = null
                try {
                    retriever.setDataSource(file.absolutePath)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } catch (_: Exception) {}

                val finalTitle = title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
                val finalArtist = artist?.takeIf { it.isNotBlank() && !it.contains("<unknown>", ignoreCase = true) } ?: "Unknown Artist"
                val finalAlbum = album?.takeIf { it.isNotBlank() && !it.contains("<unknown>", ignoreCase = true) } ?: (file.parentFile?.name ?: "Local Music")
                val finalFolder = file.parentFile?.name?.ifBlank { "Music" } ?: "Music"
                val stableId = (file.absolutePath.hashCode().toLong() and 0x7FFFFFFF).let { if (it == 0L) file.length() else it }

                result.add(
                    Song(
                        id = stableId,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        durationMs = durationMs,
                        uri = Uri.fromFile(file),
                        path = file.absolutePath,
                        isDemoTrack = false,
                        folder = finalFolder,
                        albumArtist = finalArtist,
                        genre = genre ?: "Music",
                        dateAdded = file.lastModified() / 1000L
                    )
                )
            } catch (_: Exception) {}
        }
        try { retriever.release() } catch (_: Exception) {}
        return result
    }

    /**
     * Direct file system discovery across storage directories.
     * Ensures any .mp3 file placed on device is discovered even before MediaStore indexing.
     */
    fun scanStorageFilesDirectly(context: Context): List<Song> {
        val foundSongs = mutableListOf<Song>()
        val scannedPaths = mutableSetOf<String>()

        val candidateDirectories = mutableListOf<File>()
        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.let { candidateDirectories.add(it) }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { candidateDirectories.add(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS)?.let { candidateDirectories.add(it) }
            }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)?.let { candidateDirectories.add(it) }
            Environment.getExternalStorageDirectory()?.let { candidateDirectories.add(it) }
            candidateDirectories.add(File("/storage/emulated/0/Music"))
            candidateDirectories.add(File("/storage/emulated/0/Download"))
            candidateDirectories.add(File("/sdcard/Music"))
            candidateDirectories.add(File("/sdcard/Download"))
        } catch (_: Exception) {}

        val uniqueDirs = candidateDirectories.filter { it.exists() && it.isDirectory }.distinctBy { it.absolutePath }
        val audioFilesToScan = mutableListOf<File>()

        for (dir in uniqueDirs) {
            findAudioFilesRecursive(dir, audioFilesToScan, maxDepth = 4, currentDepth = 0)
        }

        if (audioFilesToScan.isEmpty()) return emptyList()

        // Request system MediaScanner to index newly detected files in background
        try {
            val paths = audioFilesToScan.map { it.absolutePath }.toTypedArray()
            MediaScannerConnection.scanFile(context, paths, null, null)
        } catch (_: Exception) {}

        val retriever = MediaMetadataRetriever()
        for (file in audioFilesToScan) {
            if (!scannedPaths.add(file.absolutePath.lowercase())) continue
            try {
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var durationMs: Long = 0L
                var genre: String? = null

                try {
                    retriever.setDataSource(file.absolutePath)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = durStr?.toLongOrNull() ?: 0L
                } catch (_: Exception) {}

                val finalTitle = title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
                val finalArtist = artist?.takeIf { it.isNotBlank() && !it.contains("<unknown>", ignoreCase = true) } ?: "Unknown Artist"
                val finalAlbum = album?.takeIf { it.isNotBlank() && !it.contains("<unknown>", ignoreCase = true) } ?: (file.parentFile?.name ?: "Local Music")
                val finalFolder = file.parentFile?.name?.ifBlank { "Music" } ?: "Music"

                val stableId = (file.absolutePath.hashCode().toLong() and 0x7FFFFFFF).let { if (it == 0L) file.length() else it }

                foundSongs.add(
                    Song(
                        id = stableId,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        durationMs = durationMs,
                        uri = Uri.fromFile(file),
                        path = file.absolutePath,
                        isDemoTrack = false,
                        folder = finalFolder,
                        albumArtist = finalArtist,
                        genre = genre ?: "Music",
                        dateAdded = file.lastModified() / 1000L
                    )
                )
            } catch (_: Exception) {}
        }
        try { retriever.release() } catch (_: Exception) {}

        return foundSongs
    }

    private fun findAudioFilesRecursive(dir: File, result: MutableList<File>, maxDepth: Int, currentDepth: Int) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory || dir.name.startsWith(".")) return
        if (dir.name.equals("data", ignoreCase = true) || dir.name.equals("obb", ignoreCase = true)) {
            if (dir.parentFile?.name.equals("Android", ignoreCase = true)) return
        }

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                findAudioFilesRecursive(file, result, maxDepth, currentDepth + 1)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in AUDIO_EXTENSIONS && file.length() > 1024) {
                    result.add(file)
                }
            }
        }
    }

    private fun parseSongsCursor(cursor: android.database.Cursor): List<Song> {
        val list = mutableListOf<Song>()
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val rawTitle = cursor.getString(titleCol) ?: "Unknown Track"
            val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
            val rawAlbum = cursor.getString(albumCol) ?: "Unknown Album"
            val durationMs = cursor.getLong(durationCol)
            val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
            val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
            val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )
            val folderName = extractFolderName(path)

            list.add(
                Song(
                    id = id,
                    title = rawTitle,
                    artist = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else rawArtist,
                    album = if (rawAlbum.contains("<unknown>", ignoreCase = true)) "Local Music" else rawAlbum,
                    durationMs = durationMs,
                    uri = contentUri,
                    path = path,
                    isDemoTrack = false,
                    folder = folderName,
                    albumArtist = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else rawArtist,
                    albumId = albumId,
                    dateAdded = dateAdded
                )
            )
        }
        return deduplicateSongs(list)
    }

    private fun parseFoldersAndGroup(cursor: android.database.Cursor): Map<String, List<Song>> {
        val folderMap = mutableMapOf<String, MutableList<Song>>()
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val rawTitle = cursor.getString(titleCol) ?: "Unknown Track"
            val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
            val rawAlbum = cursor.getString(albumCol) ?: "Unknown Album"
            val durationMs = cursor.getLong(durationCol)
            val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
            val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
            val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )
            val folderName = extractFolderName(path)

            val song = Song(
                id = id,
                title = rawTitle,
                artist = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else rawArtist,
                album = if (rawAlbum.contains("<unknown>", ignoreCase = true)) "Local Music" else rawAlbum,
                durationMs = durationMs,
                uri = contentUri,
                path = path,
                isDemoTrack = false,
                folder = folderName,
                albumArtist = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else rawArtist,
                albumId = albumId,
                dateAdded = dateAdded
            )
            folderMap.getOrPut(folderName) { mutableListOf() }.add(song)
        }

        // Folders tree catalog: unfiltered full access, preserves all songs in each folder intact
        val sortedMap = TreeMap<String, List<Song>>(String.CASE_INSENSITIVE_ORDER)
        for ((folder, songs) in folderMap) {
            val sortedTracks = songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            sortedMap[folder] = sortedTracks
        }
        return sortedMap
    }

    private data class DeduplicationKey(
        val cleanTitle: String,
        val exactTitle: String,
        val cleanArtist: String,
        val cleanFileName: String,
        val durationMs: Long,
        val path: String
    )

    private fun cleanSongTitle(title: String): String {
        var s = title.lowercase()
        // 1. Remove text in brackets / parentheses
        s = s.replace(Regex("\\[.*?\\]|\\(.*?\\)|\\{.*?\\}"), " ")
        // 2. Remove common music website download tags & bitrate markers
        s = s.replace(Regex("(?i)\\b(pagalworld|djpunjab|songspk|naasongs|mp3mad|pendujatt|jiosaavn|wynk|gaana|spotify|youtube|remix|cover|slowed|reverb|320kbps|128kbps|64kbps|kbps|full song|lyrical|audio|video)\\b"), " ")
        // 3. Remove copy counters at the end: " (1)", " - 1", " _2", " copy", etc.
        s = s.replace(Regex("[-_ ]+(copy|\\d+)\\s*$"), " ")
        // 4. Retain all Unicode letters and digits across all scripts (Hindi, English, etc.)
        val sb = StringBuilder()
        for (ch in s) {
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    private fun exactNormSongTitle(title: String): String {
        val s = title.lowercase()
        val sb = StringBuilder()
        for (ch in s) {
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    private fun cleanArtistName(artist: String): String {
        if (artist.contains("<unknown>", ignoreCase = true) ||
            artist.isBlank() ||
            artist.equals("unknown", ignoreCase = true) ||
            artist.equals("unknown artist", ignoreCase = true) ||
            artist.equals("various artists", ignoreCase = true)) {
            return ""
        }
        val s = artist.lowercase().replace(Regex("\\[.*?\\]|\\(.*?\\)|\\{.*?\\}"), " ")
        val sb = StringBuilder()
        for (ch in s) {
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    private fun cleanFileNameFromPath(path: String): String {
        if (path.isBlank()) return ""
        return try {
            var name = File(path).nameWithoutExtension.lowercase()
            name = name.replace(Regex("\\[.*?\\]|\\(.*?\\)|\\{.*?\\}"), " ")
            name = name.replace(Regex("(?i)\\b(pagalworld|djpunjab|songspk|naasongs|mp3mad|pendujatt|remix|320kbps|128kbps|kbps)\\b"), " ")
            name = name.replace(Regex("[-_ ]+(copy|\\d+)\\s*$"), " ")
            val sb = StringBuilder()
            for (ch in name) {
                if (Character.isLetterOrDigit(ch)) {
                    sb.append(ch)
                }
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun isDuplicateTrack(a: DeduplicationKey, b: DeduplicationKey): Boolean {
        // 1. Same exact path
        if (a.path.isNotBlank() && b.path.isNotBlank() && a.path.equals(b.path, ignoreCase = true)) {
            return true
        }

        val durDiff = kotlin.math.abs(a.durationMs - b.durationMs)
        val hasCloseDuration = durDiff <= 15000L || a.durationMs <= 0 || b.durationMs <= 0

        // 2. Both have matching clean title (length >= 2)
        val sameCleanTitle = a.cleanTitle.isNotEmpty() && a.cleanTitle.length >= 2 && a.cleanTitle == b.cleanTitle
        val sameExactTitle = a.exactTitle.isNotEmpty() && a.exactTitle.length >= 2 && a.exactTitle == b.exactTitle
        val sameTitle = sameCleanTitle || sameExactTitle

        if (sameTitle) {
            // (a) Both artists are known and match (or one contains the other)
            if (a.cleanArtist.isNotEmpty() && b.cleanArtist.isNotEmpty()) {
                if (a.cleanArtist == b.cleanArtist ||
                    a.cleanArtist.contains(b.cleanArtist) ||
                    b.cleanArtist.contains(a.cleanArtist)) {
                    return true
                }
            }
            // (b) Same title and duration is very close (within 12 seconds)
            if (durDiff <= 12000L || a.durationMs <= 0 || b.durationMs <= 0) {
                return true
            }
            // (c) Either artist is unknown/blank and duration is reasonably close (within 20s)
            if ((a.cleanArtist.isEmpty() || b.cleanArtist.isEmpty()) && (durDiff <= 20000L || a.durationMs <= 0 || b.durationMs <= 0)) {
                return true
            }
        }

        // 3. Filename match: clean filename is same (length >= 3)
        // e.g. "Song (1).mp3" and "Song.mp3", or "Song - Copy.mp3"
        val sameCleanFile = a.cleanFileName.isNotEmpty() && a.cleanFileName.length >= 3 && a.cleanFileName == b.cleanFileName
        if (sameCleanFile) {
            if (hasCloseDuration) {
                return true
            }
            if (a.cleanArtist.isNotEmpty() && b.cleanArtist.isNotEmpty() &&
                (a.cleanArtist == b.cleanArtist || a.cleanArtist.contains(b.cleanArtist) || b.cleanArtist.contains(a.cleanArtist))) {
                return true
            }
        }

        // 4. Clean Title matches Clean Filename (e.g. title is "Kesariya" and filename is "kesariya (1).mp3")
        val titleMatchesFile = (a.cleanTitle.length >= 3 && a.cleanTitle == b.cleanFileName) ||
                (b.cleanTitle.length >= 3 && b.cleanTitle == a.cleanFileName)
        if (titleMatchesFile && hasCloseDuration) {
            return true
        }

        return false
    }

    /**
     * Deduplicates a list of songs by identifying matching audio signatures,
     * normalized titles, artists, duration buckets, and clean filenames.
     * Retains only 1 unique instance of any duplicated track (filtering out 2x, 4x copies).
     */
    fun deduplicateSongs(songs: List<Song>): List<Song> {
        if (songs.size <= 1) return songs
        val result = mutableListOf<Song>()
        val keysList = mutableListOf<DeduplicationKey>()

        // Fast lookup multi-maps by cleanTitle, exactTitle, and cleanFileName
        val titleIndex = HashMap<String, MutableList<Int>>()
        val fileIndex = HashMap<String, MutableList<Int>>()
        val exactTitleIndex = HashMap<String, MutableList<Int>>()

        for (song in songs) {
            val key = DeduplicationKey(
                cleanTitle = cleanSongTitle(song.title),
                exactTitle = exactNormSongTitle(song.title),
                cleanArtist = cleanArtistName(song.artist),
                cleanFileName = cleanFileNameFromPath(song.path),
                durationMs = song.durationMs,
                path = song.path
            )

            // Check if candidate matches any existing entry using indices
            var isDuplicate = false
            val candidateIndices = HashSet<Int>()

            if (key.cleanTitle.isNotEmpty()) {
                titleIndex[key.cleanTitle]?.let { candidateIndices.addAll(it) }
            }
            if (key.exactTitle.isNotEmpty()) {
                exactTitleIndex[key.exactTitle]?.let { candidateIndices.addAll(it) }
            }
            if (key.cleanFileName.isNotEmpty()) {
                fileIndex[key.cleanFileName]?.let { candidateIndices.addAll(it) }
                titleIndex[key.cleanFileName]?.let { candidateIndices.addAll(it) }
            }
            if (key.cleanTitle.isNotEmpty()) {
                fileIndex[key.cleanTitle]?.let { candidateIndices.addAll(it) }
            }

            for (idx in candidateIndices) {
                val existingKey = keysList[idx]
                if (isDuplicateTrack(key, existingKey)) {
                    isDuplicate = true
                    // If the new candidate has better metadata (e.g. non-empty artist when existing was empty),
                    // enrich existing song entry
                    val existingSong = result[idx]
                    if ((existingSong.artist.isBlank() || existingSong.artist.contains("<unknown>", ignoreCase = true)) &&
                        song.artist.isNotBlank() && !song.artist.contains("<unknown>", ignoreCase = true)) {
                        result[idx] = existingSong.copy(
                            artist = song.artist,
                            albumArtist = song.albumArtist
                        )
                    }
                    break
                }
            }

            if (!isDuplicate) {
                val newIdx = result.size
                result.add(song)
                keysList.add(key)
                if (key.cleanTitle.isNotEmpty()) {
                    titleIndex.getOrPut(key.cleanTitle) { mutableListOf() }.add(newIdx)
                }
                if (key.exactTitle.isNotEmpty()) {
                    exactTitleIndex.getOrPut(key.exactTitle) { mutableListOf() }.add(newIdx)
                }
                if (key.cleanFileName.isNotEmpty()) {
                    fileIndex.getOrPut(key.cleanFileName) { mutableListOf() }.add(newIdx)
                }
            }
        }
        return result
    }

    private fun extractFolderName(path: String): String {
        if (path.isBlank()) return "Music"
        return try {
            val parent = File(path).parentFile
            parent?.name?.ifBlank { "Music" } ?: "Music"
        } catch (t: Throwable) {
            "Music"
        }
    }

    fun getMediaStoreAudioCount(context: Context): Int {
        return try {
            val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.Audio.Media._ID),
                AUDIO_SELECTION_QUERY,
                null,
                null
            )?.use { it.count } ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
