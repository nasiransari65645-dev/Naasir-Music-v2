package com.example.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Background Service hosting and maintaining the floating Speedometer Volume HUD overlay
 * when granted SYSTEM_ALERT_WINDOW permission.
 */
class VolumeOverlayService : Service() {

    private var overlayManager: VolumeOverlayManager? = null

    companion object {
        private const val TAG = "VolumeOverlayService"
        const val ACTION_START_OVERLAY = "com.example.audio.ACTION_START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.audio.ACTION_STOP_OVERLAY"

        fun start(context: Context) {
            try {
                val intent = Intent(context, VolumeOverlayService::class.java).apply {
                    action = ACTION_START_OVERLAY
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Error starting VolumeOverlayService: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, VolumeOverlayService::class.java).apply {
                    action = ACTION_STOP_OVERLAY
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Error stopping VolumeOverlayService: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlayManager = VolumeOverlayManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                overlayManager?.dismissImmediate()
                stopSelf()
            }
            ACTION_START_OVERLAY -> {
                // Overlay manager will react to volume keys when music is actively playing
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.dismissImmediate()
    }
}
