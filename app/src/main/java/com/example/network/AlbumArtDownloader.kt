package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AlbumArtDownloader {
    private const val TAG = "AlbumArtDownloader"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Checks if device is currently connected to active internet
     * (Wi-Fi, Mobile Cellular data, Ethernet, or VPN).
     */
    fun isWifiOrMobileDataConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && (
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    )) {
                        return true
                    }
                }
                @Suppress("DEPRECATION")
                val ni = cm.activeNetworkInfo
                ni != null && (ni.isConnected || ni.isConnectedOrConnecting)
            } else {
                @Suppress("DEPRECATION")
                val ni = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                ni != null && (ni.isConnected || ni.isConnectedOrConnecting)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Network check exception, defaulting to true: ${t.message}")
            true
        }
    }

    /**
     * Cleans up song title and artist to increase search hit accuracy.
     * Strips site watermarks (Pagalworld, Songs.pk, 320kbps, brackets, etc.)
     */
    fun cleanSearchQuery(title: String, artist: String): Pair<String, String> {
        val cleanTitle = title
            .replace(Regex("(?i)\\.(mp3|m4a|wav|flac|aac|ogg)"), " ")
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), " ")
            .replace(Regex("(?i)\\b(pagalworld|pagalfree|pagalsongs|songspk|downloadming|djpunjab|pendujatt|webmusic|mymp3song|djremix|remix|ringtone|320kbps|128kbps|kbps|vbr|flac|mp3|m4a|wav|aac|ogg|320\\s*kbps|128\\s*kbps)\\b"), " ")
            .replace(Regex("(?i)\\b(official\\s*video|official\\s*audio|video\\s*song|audio\\s*song|full\\s*song|lyrical\\s*video|lyrics|lyric\\s*video|audio|video)\\b"), " ")
            .replace(Regex("(?i)\\b(feat\\.|ft\\.|featuring)\\b.*"), " ")
            .replace(Regex("^[0-9]+[\\s_\\-\\.]+"), " ") // leading track number
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val cleanArtist = artist
            .replace(Regex("(?i)<unknown>|unknown\\s*artist|unknown|various\\s*artists"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Pair(cleanTitle, cleanArtist)
    }

    /**
     * Searches iTunes Music Store API and Deezer API for high-resolution album art.
     * Saves the downloaded image to internal private app storage.
     * Returns the local file Uri string if successful, or null if failed/not found.
     */
    suspend fun downloadAlbumArt(context: Context, songId: Long, title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val isConnected = isWifiOrMobileDataConnected(context)
        if (!isConnected) {
            Log.d(TAG, "Transient network check indicated offline, attempting download with fast timeout anyway")
        }

        val (cleanTitle, cleanArtist) = cleanSearchQuery(title, artist)
        val queriesToTry = mutableListOf<String>()

        if (cleanTitle.isNotBlank()) {
            if (cleanArtist.isNotBlank()) {
                queriesToTry.add("$cleanTitle $cleanArtist")
            }
            queriesToTry.add(cleanTitle)
        }

        // If title contained hyphens (e.g. "Artist - Track" or "Movie - Track")
        if (title.contains("-")) {
            val parts = title.split("-")
                .map { it.replace(Regex("(?i)\\.(mp3|m4a|wav)"), "").trim() }
                .filter { it.length >= 2 }
            if (parts.size >= 2) {
                queriesToTry.add("${parts[0]} ${parts[1]}")
                queriesToTry.add("${parts[1]} ${parts[0]}")
                queriesToTry.add(parts[0])
                queriesToTry.add(parts[1])
            }
        }

        var foundArtworkUrl: String? = null

        for (q in queriesToTry.distinct()) {
            if (q.isBlank()) continue

            // 1. Try iTunes Music API
            foundArtworkUrl = searchItunes(q)
            if (!foundArtworkUrl.isNullOrBlank()) {
                Log.d(TAG, "Found iTunes artwork for query '$q': $foundArtworkUrl")
                break
            }

            // 2. Fallback to Deezer API
            foundArtworkUrl = searchDeezer(q)
            if (!foundArtworkUrl.isNullOrBlank()) {
                Log.d(TAG, "Found Deezer artwork for query '$q': $foundArtworkUrl")
                break
            }
        }

        if (foundArtworkUrl.isNullOrBlank()) {
            Log.d(TAG, "No album artwork found online for '$cleanTitle' ('$cleanArtist')")
            return@withContext null
        }

        // 3. Download the image bytes and save to app filesDir
        try {
            val artDir = File(context.filesDir, "album_art").apply { mkdirs() }
            val targetFile = File(artDir, "art_${songId}.jpg")

            val downloadRequest = Request.Builder()
                .url(foundArtworkUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(downloadRequest).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Artwork download HTTP failed: ${response.code}")
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 500) {
                val fileUri = Uri.fromFile(targetFile).toString()
                Log.i(TAG, "Successfully downloaded album art for '$title' to $fileUri")
                return@withContext fileUri
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed downloading artwork image: ${t.message}", t)
        }

        return@withContext null
    }

    private fun searchItunes(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&media=music&limit=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null

            val json = JSONObject(bodyString)
            if (json.optInt("resultCount", 0) <= 0) return null
            val results = json.optJSONArray("results") ?: return null
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val artwork100 = item.optString("artworkUrl100")
                if (artwork100.isNotBlank()) {
                    // Upgrade resolution to 600x600 for sharp display
                    return artwork100.replace("100x100bb.jpg", "600x600bb.jpg")
                        .replace("100x100", "600x600")
                }
            }
            null
        } catch (t: Throwable) {
            Log.w(TAG, "iTunes search error for '$query': ${t.message}")
            null
        }
    }

    private fun searchDeezer(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.deezer.com/search?q=$encoded&limit=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null

            val json = JSONObject(bodyString)
            val data = json.optJSONArray("data") ?: return null
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val album = item.optJSONObject("album")
                if (album != null) {
                    val coverXl = album.optString("cover_xl")
                    if (coverXl.isNotBlank()) return coverXl
                    val coverBig = album.optString("cover_big")
                    if (coverBig.isNotBlank()) return coverBig
                    val coverMedium = album.optString("cover_medium")
                    if (coverMedium.isNotBlank()) return coverMedium
                }
            }
            null
        } catch (t: Throwable) {
            Log.w(TAG, "Deezer search error for '$query': ${t.message}")
            null
        }
    }
}
