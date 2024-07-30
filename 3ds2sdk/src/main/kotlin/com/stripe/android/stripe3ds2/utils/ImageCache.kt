package com.stripe.android.stripe3ds2.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import kotlin.math.min

internal interface ImageCache {
    fun clear()
    operator fun get(key: String): Bitmap?
    operator fun set(key: String, bitmap: Bitmap)

    object Default : ImageCache {
        private const val KB = 1024

        // max size of image cache in kilobytes
        private const val MAX_SIZE = 10240 // 10MB

        // Get max available VM memory, only use 1/8th of available memory
        private val cacheSize = min(
            (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
            MAX_SIZE
        )

        @VisibleForTesting
        internal val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                return bitmap.byteCount / KB
            }
        }

        override operator fun get(key: String): Bitmap? {
            return cache.get(key)
        }

        override operator fun set(key: String, bitmap: Bitmap) {
            cache.put(key, bitmap)
        }

        override fun clear() {
            cache.evictAll()
        }
    }
}
