package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
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

    /**
     * Built-in Zero-Touch Automatic Media Scanning Architecture:
     * Applies two distinct pipelines in a single scan pass:
     *
     * Pipeline A: Main Songs Catalog (Auto-Filtered & Auto-Sorted)
     * - Only complete music tracks: IS_MUSIC != 0 AND DURATION >= 15000 (filters out < 15s clips)
     * - Native Auto-Sort Order: DATE_ADDED DESC (newest tracks at top)
     *
     * Pipeline B: Folder Tree Catalog (Unfiltered Full Access)
     * - Complete directory structure: DURATION > 0 (all audio intact)
     * - Folders sorted alphabetically (A to Z)
     * - Tracks inside folders sorted alphabetically by title
     */
    suspend fun scanMediaAutomatically(context: Context): ScannedAudioResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // --- Pipeline A: Main Songs Catalog (Auto-Filtered & Auto-Sorted) ---
        val autoSortedSongs = mutableListOf<Song>()
        try {
            contentResolver.query(
                collection,
                songProjection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?",
                arrayOf("15000"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                autoSortedSongs.addAll(parseSongsCursor(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Pipeline A (Main Songs): ${e.message}", e)
        }

        // --- Pipeline B: Folder Tree Catalog (Unfiltered Full Access) ---
        var autoOrganizedFolders: Map<String, List<Song>> = emptyMap()
        try {
            contentResolver.query(
                collection,
                folderProjection,
                "${MediaStore.Audio.Media.DURATION} > 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                autoOrganizedFolders = parseFoldersAndGroup(cursor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Pipeline B (Folder Tree): ${e.message}", e)
        }

        val result = ScannedAudioResult(songs = autoSortedSongs, folders = autoOrganizedFolders)
        cachedResult = result
        result
    }

    /**
     * Primary entry point invoked by ViewModel.
     * Executes the automatic dual-pipeline scan and returns the auto-sorted main songs catalog.
     */
    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val result = scanMediaAutomatically(context)
        result.songs
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

    /**
     * Generates robust fingerprint keys for detecting duplicates across differing folders,
     * download numbers like (1), bracketed video tags, and duplicate MediaStore entries.
     */
    fun generateDeduplicationSignatures(
        title: String,
        artist: String,
        durationMs: Long,
        path: String
    ): List<String> {
        val signatures = mutableListOf<String>()
        val exactNormTitle = exactNormSongTitle(title)
        val strippedTitle = cleanSongTitle(title)
        val normArtist = cleanArtistName(artist)
        val durationBucket = durationMs / 10000L // 10-second tolerance bucket

        if (exactNormTitle.isNotBlank()) {
            if (normArtist.isNotBlank()) {
                signatures.add("ta:$exactNormTitle|$normArtist")
            }
            signatures.add("t:$exactNormTitle|$durationBucket")
        }

        if (strippedTitle.isNotBlank() && strippedTitle != exactNormTitle) {
            if (normArtist.isNotBlank()) {
                signatures.add("sta:$strippedTitle|$normArtist")
            }
            signatures.add("st:$strippedTitle|$durationBucket")
        }

        val fileName = cleanFileNameFromPath(path)
        if (fileName.length >= 3) {
            signatures.add("f:$fileName|$durationBucket")
        }
        return signatures
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

    suspend fun loadSimpleMusic(context: Context): List<Song> = withContext(Dispatchers.IO) {
        emptyList()
    }
}
