package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    suspend fun getAllSongsSync(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT MAX(dateAdded) FROM songs")
    suspend fun getLatestDateAdded(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:validIds)")
    suspend fun deleteRemovedSongs(validIds: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAll()
}
