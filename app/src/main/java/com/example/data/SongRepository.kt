package com.example.data

import com.example.database.SongMetadataDao
import com.example.database.SongMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SongRepository(private val dao: SongMetadataDao) {

    val allMetadata: Flow<Map<Long, SongMetadataEntity>> = dao.getAllMetadata().map { list ->
        list.associateBy { it.songId }
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
