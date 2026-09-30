package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_metadata")
data class SongMetadataEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbumArtUri: String? = null,
    val isDeleted: Boolean = false,
    val autoArtDownloaded: Boolean = false
)
