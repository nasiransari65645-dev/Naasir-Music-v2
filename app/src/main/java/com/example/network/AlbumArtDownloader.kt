package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AlbumArtDownloader {
    private const val TAG = "AlbumArtDownloader"

    /**
     * Checks if device is currently connected to Wi-Fi or Mobile Cellular data.
     */
    fun isWifiOrMobileDataConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (t: Throwable) {
            Log.e(TAG, "Network capability check failed: ${t.message}")
            false
        }
    }

    /**
     * Cleans up song title and artist to increase search hit accuracy.
     */
    fun cleanSearchQuery(title: String, artist: String): String {
        var cleanTitle = title
            .replace(Regex("(?i)\\.mp3|\\.m4a|\\.wav|\\.flac|\\.aac|\\.ogg"), " ")
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), " ")
            .replace(Regex("(?i)feat\\..*|ft\\..*"), " ")
            .replace(Regex("^[0-9]+[\\s_\\-\\.]+"), " ") // leading track number
            .replace(Regex("[_\\-]+"), " ")
            .trim()

        val cleanArtist = artist
            .replace(Regex("(?i)<unknown>|unknown artist|unknown"), "")
            .trim()

        return if (cleanArtist.isNotBlank()) {
            "$cleanTitle $cleanArtist".trim()
        } else {
            cleanTitle
        }
    }

    /**
     * Searches iTunes Music Store API and downloads high-resolution (600x600) album art.
     * Saves the downloaded image to internal private app storage.
     * Returns the local file Uri string if successful, or null if failed/not found.
     */
    suspend fun downloadAlbumArt(context: Context, songId: Long, title: String, artist: String): String? = withContext(Dispatchers.IO) {
        if (!isWifiOrMobileDataConnected(context)) {
            Log.d(TAG, "Skipping album art download: No Wi-Fi or Mobile data connected")
            return@withContext null
        }

        val query = cleanSearchQuery(title, artist)
        if (query.isBlank()) return@withContext null

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NaasirMusic/1.0 (Android)")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Search API responded with status ${connection.responseCode}")
                connection.disconnect()
                return@withContext null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseBody)
            val resultCount = json.optInt("resultCount", 0)
            if (resultCount <= 0) {
                Log.d(TAG, "No iTunes album art found for query: $query")
                return@withContext null
            }

            val resultsArray = json.optJSONArray("results") ?: return@withContext null
            val firstResult = resultsArray.optJSONObject(0) ?: return@withContext null
            val artworkUrl100 = firstResult.optString("artworkUrl100")
            if (artworkUrl100.isBlank()) return@withContext null

            // Upgrade resolution from 100x100 to 600x600 for sharp display
            val highResUrl = artworkUrl100.replace("100x100bb.jpg", "600x600bb.jpg")
                .replace("100x100", "600x600")

            // Download image to app private storage
            val artDir = File(context.filesDir, "album_art").apply { mkdirs() }
            val targetFile = File(artDir, "art_${songId}.jpg")

            val imgUrl = URL(highResUrl)
            val imgConn = (imgUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (imgConn.responseCode == HttpURLConnection.HTTP_OK) {
                imgConn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                imgConn.disconnect()
                Log.i(TAG, "Successfully auto-downloaded album art for '$title' to ${targetFile.absolutePath}")
                return@withContext Uri.fromFile(targetFile).toString()
            } else {
                imgConn.disconnect()
                return@withContext null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to download album art for song $songId ('$title'): ${t.message}")
            null
        }
    }
}
