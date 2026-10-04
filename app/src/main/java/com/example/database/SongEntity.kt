package com.example.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "cached_songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumId: Long = 0L,
    val customAlbumArtUri: String? = null,
    val dateAdded: Long = 0L,
    val path: String = "",
    val genre: String = "Unknown Genre",
    val composer: String = "Unknown Composer",
    val folder: String = "Music"
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = Uri.parse(uriString),
            path = path,
            albumId = albumId,
            customAlbumArtUri = customAlbumArtUri,
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
                durationMs = song.durationMs,
                uriString = song.uri.toString(),
                albumId = song.albumId,
                customAlbumArtUri = song.customAlbumArtUri,
                dateAdded = song.dateAdded,
                path = song.path,
                genre = song.genre,
                composer = song.composer,
                folder = song.folder
            )
        }
    }
}
