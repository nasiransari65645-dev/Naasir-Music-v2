package com.example.model

import androidx.compose.runtime.Immutable

/**
 * Pre-indexed fast category cache holding ready-to-display grouped lists
 * and maps for instant 0ms tab switching and filtering.
 */
@Immutable
data class CategoryIndex(
    val artistsList: List<Pair<String, List<Song>>> = emptyList(),
    val albumsList: List<Pair<String, List<Song>>> = emptyList(),
    val foldersList: List<Pair<String, List<Song>>> = emptyList(),
    val genresList: List<Pair<String, List<Song>>> = emptyList(),
    val albumArtistsList: List<Pair<String, List<Song>>> = emptyList(),
    val composersList: List<Pair<String, List<Song>>> = emptyList(),
    val mostPlayedList: List<Song> = emptyList(),
    val artistSongsMap: Map<String, List<Song>> = emptyMap(),
    val albumSongsMap: Map<String, List<Song>> = emptyMap(),
    val folderSongsMap: Map<String, List<Song>> = emptyMap(),
    val genreSongsMap: Map<String, List<Song>> = emptyMap()
)
