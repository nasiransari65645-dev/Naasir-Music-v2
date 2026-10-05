package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * High-performance JSON-based disk cache manager for scanned songs.
 * Stores song catalog in app-private internal storage: File(context.filesDir, "songs_cache.json")
 * Enables 0ms instant song list rendering upon application launch without querying MediaStore.
 */
class SongCacheManager(private val context: Context) {

    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

    /**
     * Reads and parses cached songs from the private local JSON file on Dispatchers.IO.
     * Returns null if the cache file does not exist, is empty, or encounters parse errors.
     */
    suspend fun getCachedSongs(): List<Song>? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            return@withContext null
        }
        try {
            val jsonText = cacheFile.readText(Charsets.UTF_8)
            if (jsonText.isBlank()) return@withContext null

            val jsonArray = JSONArray(jsonText)
            val count = jsonArray.length()
            if (count == 0) return@withContext null

            val songs = ArrayList<Song>(count)
            for (i in 0 until count) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getLong("id")
                val title = obj.optString("title", "Unknown Track")
                val artist = obj.optString("artist", "Unknown Artist")
                val album = obj.optString("album", "Local Music")
                val durationMs = obj.optLong("durationMs", 0L)
                val uriStr = obj.optString("uri", "")
                val path = obj.optString("path", "")
                val isDemoTrack = obj.optBoolean("isDemoTrack", false)
                val genre = obj.optString("genre", "Unknown Genre")
                val composer = obj.optString("composer", "Unknown Composer")
                val albumArtist = obj.optString("albumArtist", artist)
                val folder = obj.optString("folder", "Music")
                val albumId = obj.optLong("albumId", 0L)
                val dateAdded = obj.optLong("dateAdded", 0L)
                val customAlbumArtUri = if (obj.has("customAlbumArtUri") && !obj.isNull("customAlbumArtUri")) {
                    obj.getString("customAlbumArtUri")
                } else null
                val playCount = obj.optInt("playCount", 0)

                val parsedUri = if (uriStr.isNotBlank()) {
                    try {
                        Uri.parse(uriStr)
                    } catch (_: Throwable) {
                        Uri.EMPTY
                    }
                } else {
                    Uri.EMPTY
                }

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        uri = parsedUri,
                        path = path,
                        isDemoTrack = isDemoTrack,
                        genre = genre,
                        composer = composer,
                        albumArtist = albumArtist,
                        folder = folder,
                        albumId = albumId,
                        dateAdded = dateAdded,
                        customAlbumArtUri = customAlbumArtUri,
                        playCount = playCount
                    )
                )
            }
            Log.d(TAG, "Successfully loaded ${songs.size} cached songs from JSON in 0ms")
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached songs JSON: ${e.message}", e)
            null
        }
    }

    /**
     * Serializes songs to JSON and writes to disk atomically via a temporary file on Dispatchers.IO.
     */
    suspend fun saveSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext
        try {
            val jsonArray = JSONArray()
            for (song in songs) {
                val obj = JSONObject()
                obj.put("id", song.id)
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("album", song.album)
                obj.put("durationMs", song.durationMs)
                obj.put("uri", song.uri.toString())
                obj.put("path", song.path)
                obj.put("isDemoTrack", song.isDemoTrack)
                obj.put("genre", song.genre)
                obj.put("composer", song.composer)
                obj.put("albumArtist", song.albumArtist)
                obj.put("folder", song.folder)
                obj.put("albumId", song.albumId)
                obj.put("dateAdded", song.dateAdded)
                if (song.customAlbumArtUri != null) {
                    obj.put("customAlbumArtUri", song.customAlbumArtUri)
                }
                obj.put("playCount", song.playCount)
                jsonArray.put(obj)
            }

            // Write atomically via temporary file
            val tempFile = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tempFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            if (!tempFile.renameTo(cacheFile)) {
                cacheFile.delete()
                if (!tempFile.renameTo(cacheFile)) {
                    cacheFile.writeText(jsonArray.toString(), Charsets.UTF_8)
                    tempFile.delete()
                }
            }
            Log.d(TAG, "Saved ${songs.size} songs to $CACHE_FILE_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cached songs JSON: ${e.message}", e)
        }
    }

    /**
     * Clears the JSON cache file upon manual rescan or library reset.
     */
    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            val tempFile = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Log.d(TAG, "Cleared $CACHE_FILE_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "SongCacheManager"
        const val CACHE_FILE_NAME = "songs_cache.json"
    }
}
