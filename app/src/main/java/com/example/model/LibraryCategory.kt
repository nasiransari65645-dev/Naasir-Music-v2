package com.example.model

enum class LibraryCategory(val title: String) {
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    SONGS("Songs"),
    FOLDERS("Folder"),
    PLAYLISTS("Playlists"),
    GENRE("Genre"),
    ALBUM_ARTISTS("Album Artists"),
    COMPILATIONS("Compilations"),
    COMPOSERS("Composers");

    companion object {
        val ALL_SONGS = SONGS
        val FAVORITES = PLAYLISTS
        val SIMPLE_MUSIC = SONGS
        val QUICK_PICKS = SONGS
    }
}
