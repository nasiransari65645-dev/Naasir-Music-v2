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
        return list
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

        // Sort folders alphabetically (A to Z) and sort tracks inside folders by track title
        val sortedMap = TreeMap<String, List<Song>>(String.CASE_INSENSITIVE_ORDER)
        for ((folder, songs) in folderMap) {
            val sortedTracks = songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            sortedMap[folder] = sortedTracks
        }
        return sortedMap
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
