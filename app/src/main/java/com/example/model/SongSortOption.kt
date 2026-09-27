package com.example.model

enum class SongSortOption(val displayName: String) {
    DATE_ADDED_DESC("Date Added (Newest First)"),
    TITLE_A_TO_Z("Title (A to Z)"),
    TITLE_Z_TO_A("Title (Z to A)"),
    DURATION_LONGEST_FIRST("Duration (Longest first)"),
    ARTIST_NAME("Artist Name");

    companion object {
        // Backwards compatibility alias for RECENTLY_ADDED
        val RECENTLY_ADDED get() = DATE_ADDED_DESC
    }
}
