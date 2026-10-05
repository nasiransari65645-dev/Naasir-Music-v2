package com.example.audio

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time MediaStore ContentObserver that automatically detects when new songs
 * are added or downloaded to device storage, debounces bursts, and triggers
 * silent incremental Room DB synchronization.
 */
class MediaContentObserver(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onMediaChanged: suspend () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var debounceJob: Job? = null
    private var isRegistered = false

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "MediaStore audio change detected: uri=$uri, selfChange=$selfChange")
        // Debounce ~1.5s in case multiple audio files are downloaded sequentially
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(1500)
            try {
                onMediaChanged()
            } catch (t: Throwable) {
                Log.e(TAG, "Error in onMediaChanged: ${t.message}", t)
            }
        }
    }

    fun register() {
        if (!isRegistered) {
            try {
                context.contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    this
                )
                isRegistered = true
                Log.d(TAG, "MediaContentObserver registered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register MediaContentObserver: ${e.message}", e)
            }
        }
    }

    fun unregister() {
        if (isRegistered) {
            try {
                debounceJob?.cancel()
                context.contentResolver.unregisterContentObserver(this)
                isRegistered = false
                Log.d(TAG, "MediaContentObserver unregistered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister MediaContentObserver: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "MediaContentObserver"
    }
}
