package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongMetadataDao {
    @Query("SELECT * FROM song_metadata")
    fun getAllMetadata(): Flow<List<SongMetadataEntity>>

    @Query("SELECT * FROM song_metadata WHERE songId = :songId LIMIT 1")
    suspend fun getMetadata(songId: Long): SongMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SongMetadataEntity)

    @Query("UPDATE song_metadata SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE songId = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long): Int

    @Query("UPDATE song_metadata SET customTitle = :title, customArtist = :artist WHERE songId = :songId")
    suspend fun updateTitleAndArtist(songId: Long, title: String?, artist: String?): Int

    @Query("UPDATE song_metadata SET customAlbumArtUri = :uri, autoArtDownloaded = 1 WHERE songId = :songId")
    suspend fun updateAlbumArt(songId: Long, uri: String?): Int

    @Query("UPDATE song_metadata SET isDeleted = 1 WHERE songId = :songId")
    suspend fun markDeleted(songId: Long): Int
}
