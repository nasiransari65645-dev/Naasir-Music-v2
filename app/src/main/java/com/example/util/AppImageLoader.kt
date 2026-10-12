package com.example.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

object AppImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context.applicationContext).also { instance = it }
        }
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.35) // Up to 35% RAM dedicated to cached thumbnails
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("album_art_disk_cache"))
                    .maxSizePercent(0.05) // Up to 5% disk storage
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(false) // Zero-lag immediate render without crossfade delay
            .build()
    }
}
