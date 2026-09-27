package com.example.model

import android.content.ContentUris
import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val path: String = "",
    val isDemoTrack: Boolean = false,
    val genre: String = "Unknown Genre",
    val composer: String = "Unknown Composer",
    val albumArtist: String = "Unknown Artist",
    val folder: String = "Music",
    val albumId: Long = 0L,
    val dateAdded: Long = 0L
) {
    val contentUri: Uri
        get() = uri

    val albumArtUri: Uri?
        get() = if (albumId > 0) {
            try {
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            } catch (t: Throwable) {
                null
            }
        } else null

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

enum class AutoRotateDirection(val displayName: String) {
    LEFT_TO_RIGHT("Left ➔ Right"),
    RIGHT_TO_LEFT("Right ➔ Left")
}

enum class SpatialEnvironment(
    val displayName: String,
    val description: String,
    val decayLabel: String,
    val decaySeconds: Float,
    val wetMix: Float
) {
    STUDIO_DRY("Studio Dry", "Crisp direct sound with tight acoustic absorption and zero reverb", "Decay 0.5s • Wet 0%", 0.5f, 0.0f),
    ACOUSTIC_ROOM("Acoustic Room", "Natural intimate reflections offering spatial presence", "Decay 1.2s • Wet 30%", 1.2f, 0.30f),
    LIVE_STAGE("Live Stage", "Dynamic open-air stage presence with warm punch", "Decay 2.4s • Wet 45%", 2.4f, 0.45f),
    CONCERT_HALL("Concert Hall", "Medium decay, rich symphonic hall reflections", "Decay 3.2s • Wet 60%", 3.2f, 0.60f),
    GREAT_HALL("Cathedral", "Expansive spatial tail with deep harmonic decay", "Decay 6.0s • Wet 85%", 6.0f, 0.85f),
    MEGA_STADIUM("Large Arena", "Colossal open-arena resonance with thunderous decay", "Decay 5.5s • Wet 75%", 5.5f, 0.75f),
    ECHO_CHAMBER("Echo Chamber", "Distinct rhythmic slap-back echo with flutter resonance", "Decay 4.0s • Wet 70%", 4.0f, 0.70f)
}

enum class DpsProfile(
    val displayName: String,
    val bassGain: Short,       // BassBoost strength (0-1000)
    val loudnessTarget: Int,   // LoudnessEnhancer targetGain in mB
    val clarityBoostDb: Int,   // Treble EQ boost
    val statusDescription: String
) {
    MUSIC("Music", 350, 450, 2, "Dynamic studio mastering with zero distortion"),
    MOVIE("Movie", 750, 800, 3, "Cinematic thunderous sub-bass & dialogue presence"),
    VOICE("Voice", 0, 300, 5, "Vocal focus & speech clarity with low-cut filter"),
    GAMING("Gaming", 500, 600, 6, "High-transient spatial response for competitive audio");

    val description: String get() = statusDescription
}
