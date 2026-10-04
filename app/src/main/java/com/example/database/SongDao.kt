package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM cached_songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM cached_songs ORDER BY dateAdded DESC")
    suspend fun getAllSongsSync(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM cached_songs")
    suspend fun getSongCount(): Int

    @Query("SELECT MAX(dateAdded) FROM cached_songs")
    suspend fun getLatestDateAdded(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM cached_songs WHERE id NOT IN (:validIds)")
    suspend fun deleteRemovedSongs(validIds: List<Long>)

    @Query("DELETE FROM cached_songs")
    suspend fun clearAll()
}
