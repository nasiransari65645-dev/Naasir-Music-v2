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

    val allMetadata: Flow<Map<Long, SongMetadataEntity>> = dao.getAllMetadata().map { list ->
        list.associateBy { it.songId }
    }

    /**
     * Instantly loads cached songs from Room DB in 0ms without waiting for MediaStore.
     */
    suspend fun getCachedSongsSync(): List<Song> = withContext(Dispatchers.IO) {
        songDao?.getAllSongsSync()?.map { it.toSong() } ?: emptyList()
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
     * Efficiently syncs with MediaStore:
     * - Fast path: Checks MediaStore audio count. If count matches Room DB count and !forceRescan, returns cached songs immediately (0ms).
     * - Refresh path: If files changed or forceRescan is requested, queries MediaStore, updates Room DB, prunes removed songs, and returns updated list.
     */
    suspend fun syncWithMediaStore(forceRescan: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        val cached = songDao?.getAllSongsSync() ?: emptyList()

        if (!forceRescan && cached.isNotEmpty()) {
            val mediaCount = AudioScanner.getMediaStoreAudioCount(ctx)
            if (mediaCount == cached.size) {
                // Media store count matches local Room cache perfectly -> zero re-scan delay
                return@withContext cached.map { it.toSong() }
            }
        }

        // Full scan: Audio files changed or forced manual refresh
        val scanned = AudioScanner.scanDeviceAudio(ctx)
        if (scanned.isNotEmpty() && songDao != null) {
            val entities = scanned.map { song ->
                SongEntity.fromSong(song)
            }
            songDao.insertSongs(entities)
            songDao.deleteRemovedSongs(scanned.map { it.id })
            return@withContext scanned
        }

        if (scanned.isEmpty() && cached.isNotEmpty()) {
            return@withContext cached.map { it.toSong() }
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
