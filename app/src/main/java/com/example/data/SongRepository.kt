package com.example.data

import android.content.Context
import com.example.audio.AudioScanner
import com.example.database.SongDao
import com.example.database.SongEntity
import com.example.database.SongMetadataDao
import com.example.database.SongMetadataEntity
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SongRepository(
    private val dao: SongMetadataDao,
    private val songDao: SongDao? = null,
    private val context: Context? = null
) {
    val songCacheManager: SongCacheManager? = context?.let { SongCacheManager(it) }

    val allMetadata: Flow<Map<Long, SongMetadataEntity>> = dao.getAllMetadata().map { list ->
        list.associateBy { it.songId }
    }

    /**
     * Reactive flow of cached songs from Room Database for instant UI updates.
     */
    val allSongs: Flow<List<Song>> = getCachedSongsFlow()

    /**
     * Primary instant cache reader: checks SongCacheManager (songs_cache.json) for 0ms instant load.
     * Falls back to Room DB if JSON is empty, and updates SongCacheManager.
     */
    suspend fun getCachedSongs(): List<Song>? = withContext(Dispatchers.IO) {
        // Step 1: Read from SongCacheManager
        val jsonCached = songCacheManager?.getCachedSongs()
        if (!jsonCached.isNullOrEmpty()) {
            return@withContext jsonCached
        }
        // Step 2: Fallback to Room DB if JSON is empty
        val roomCached = songDao?.getAllSongsSync()?.map { it.toSong() }
        if (!roomCached.isNullOrEmpty()) {
            songCacheManager?.saveSongs(roomCached)
            return@withContext roomCached
        }
        null
    }

    /**
     * Saves songs to both local JSON disk cache and Room DB.
     */
    suspend fun saveSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        songCacheManager?.saveSongs(songs)
        if (songDao != null && songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromSong(it) }
            songDao.insertSongs(entities)
        }
    }

    /**
     * Clears both local disk file cache and Room DB.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        songCacheManager?.clearCache()
        songDao?.clearAll()
    }

    /**
     * Manual rescan: clears local cache and performs a full scan of device audio.
     */
    suspend fun rescanLibrary(): List<Song> = withContext(Dispatchers.IO) {
        clearCache()
        syncWithMediaStore(forceRescan = true)
    }

    /**
     * Silent background scan: checks device audio without blocking UI or showing progress spinner.
     * Compares MediaStore count with cached count. If matching and not forced, returns cached in 0ms.
     * Otherwise queries MediaStore in background, updates disk and Room cache, and returns fresh list.
     */
    suspend fun scanDeviceAudioSilently(): List<Song> = withContext(Dispatchers.IO) {
        syncWithMediaStore(forceRescan = false)
    }

    /**
     * Instantly loads cached songs from Room DB or JSON in 0ms without waiting for MediaStore.
     */
    suspend fun getCachedSongsSync(): List<Song> = withContext(Dispatchers.IO) {
        getCachedSongs() ?: emptyList()
    }

    /**
     * Reactive flow of cached songs from Room Database.
     */
    fun getCachedSongsFlow(): Flow<List<Song>> {
        return (songDao?.getAllSongs() ?: kotlinx.coroutines.flow.emptyFlow()).map { entities ->
            entities.map { it.toSong() }
        }
    }

    /**
     * Incremental sync query: queries MediaStore ONLY for songs with DATE_ADDED > latest dateAdded.
     * Inserts new songs into Room & fast disk cache (which automatically pushes updates via Room Flow).
     * Also checks if songs have been removed from storage and prunes them cleanly.
     */
    suspend fun syncNewSongs(): List<Song> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        val latestTimestamp = songDao?.getLatestDateAdded() ?: 0L

        if (latestTimestamp <= 0L) {
            // First time sync or empty DB: perform full sync
            return@withContext syncWithMediaStore(forceRescan = true)
        }

        val newSongs = AudioScanner.scanDeviceAudioAfter(ctx, latestTimestamp)
        if (newSongs.isNotEmpty() && songDao != null) {
            val entities = newSongs.map { SongEntity.fromSong(it) }
            songDao.insertSongs(entities)
            val updatedAll = songDao.getAllSongsSync().map { it.toSong() }
            songCacheManager?.saveSongs(updatedAll)
        }

        // Check if any deleted files exist
        val dbCount = songDao?.getSongCount() ?: 0
        val mediaStoreCount = AudioScanner.getMediaStoreAudioCount(ctx)
        if (mediaStoreCount < dbCount) {
            syncWithMediaStore(forceRescan = true)
        } else {
            getCachedSongsSync()
        }
    }

    /**
     * Efficiently syncs with MediaStore:
     * - Fast path: Checks MediaStore audio count. If count matches Room DB count and !forceRescan, returns cached songs immediately (0ms).
     * - Refresh path: If files changed or forceRescan is requested, queries MediaStore, updates Room DB & JSON cache, prunes removed songs, and returns updated list.
     */
    suspend fun syncWithMediaStore(forceRescan: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        val cached = getCachedSongs() ?: emptyList()

        if (!forceRescan && cached.isNotEmpty()) {
            val mediaCount = AudioScanner.getMediaStoreAudioCount(ctx)
            if (mediaCount == cached.size) {
                // Media store count matches local cache perfectly -> zero re-scan delay
                return@withContext cached
            }
        }

        // Full scan: Audio files changed or forced manual refresh
        val scanned = AudioScanner.scanDeviceAudio(ctx)
        if (scanned.isNotEmpty()) {
            if (songDao != null) {
                val entities = scanned.map { song ->
                    SongEntity.fromSong(song)
                }
                songDao.insertSongs(entities)
                songDao.deleteRemovedSongs(scanned.map { it.id })
            }
            songCacheManager?.saveSongs(scanned)
            return@withContext scanned
        }

        if (scanned.isEmpty() && cached.isNotEmpty()) {
            return@withContext cached
        }

        scanned
    }

    suspend fun getMetadata(songId: Long): SongMetadataEntity? = withContext(Dispatchers.IO) {
        dao.getMetadata(songId)
    }

    suspend fun recordSongPlayed(songId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val rows = dao.incrementPlayCount(songId, now)
        if (rows == 0) {
            dao.insertOrUpdate(
                SongMetadataEntity(
                    songId = songId,
                    playCount = 1,
                    lastPlayedTimestamp = now
                )
            )
        }
    }

    suspend fun incrementPlayCount(songId: Long) = recordSongPlayed(songId)

    suspend fun renameSong(songId: Long, newTitle: String, newArtist: String) = withContext(Dispatchers.IO) {
        val rows = dao.updateTitleAndArtist(songId, newTitle.trim(), newArtist.trim())
        if (rows == 0) {
            dao.insertOrUpdate(
                SongMetadataEntity(
                    songId = songId,
                    customTitle = newTitle.trim(),
                    customArtist = newArtist.trim()
                )
            )
        }
    }

    suspend fun deleteSong(songId: Long) = withContext(Dispatchers.IO) {
        val rows = dao.markDeleted(songId)
        if (rows == 0) {
            dao.insertOrUpdate(
                SongMetadataEntity(
                    songId = songId,
                    isDeleted = true
                )
            )
        }
    }

    suspend fun setCustomAlbumArt(songId: Long, artUri: String) = withContext(Dispatchers.IO) {
        val rows = dao.updateAlbumArt(songId, artUri)
        if (rows == 0) {
            dao.insertOrUpdate(
                SongMetadataEntity(
                    songId = songId,
                    customAlbumArtUri = artUri,
                    autoArtDownloaded = true
                )
            )
        }
    }
}
