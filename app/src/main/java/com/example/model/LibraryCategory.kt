package com.example.model

enum class LibraryCategory(val title: String) {
    FAVORITES("Favorites"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    SONGS("Songs"),
    MOST_PLAYED("Most Played"),
    FOLDERS("Folder"),
    PLAYLISTS("Playlists"),
    GENRE("Genre"),
    ALBUM_ARTISTS("Album Artists"),
    COMPILATIONS("Compilations"),
    COMPOSERS("Composers");

    companion object {
        val ALL_SONGS = SONGS
        val SIMPLE_MUSIC = SONGS
        val QUICK_PICKS = SONGS
    }
}
