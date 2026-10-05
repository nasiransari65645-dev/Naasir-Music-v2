package com.example.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val path: String = "",
    val genre: String = "Unknown Genre",
    val composer: String = "Unknown Composer",
    val folder: String = "Music",
    val albumId: Long = 0L
) {
    val durationMs: Long
        get() = duration

    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            uri = Uri.parse(uri),
            path = path,
            albumId = albumId,
            customAlbumArtUri = albumArtUri,
            genre = genre,
            composer = composer,
            folder = folder,
            dateAdded = dateAdded
        )
    }

    companion object {
        fun fromSong(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.durationMs,
                uri = song.uri.toString(),
                albumArtUri = song.customAlbumArtUri ?: song.albumArtUri?.toString(),
                dateAdded = song.dateAdded,
                path = song.path,
                genre = song.genre,
                composer = song.composer,
                folder = song.folder,
                albumId = song.albumId
            )
        }
    }
}
