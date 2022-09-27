package com.stripe.android.uicore.image

import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ImageLruMemoryCache {

    @Suppress("MagicNumber")
    private val lruCache = object : LruCache<String, Bitmap>(
        // Use 1/8th of the available memory for this memory cache.
        (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        synchronized(this) {
            if (lruCache.get(key.toKey()) == null) {
                lruCache.put(key.toKey(), bitmap)
            }
        }
    }

    fun getBitmap(key: String): Bitmap? {
        synchronized(this) {
            return lruCache.get(key.toKey())
        }
    }

    fun clear() {
        synchronized(this) {
            lruCache.evictAll()
        }
    }

    private fun String.toKey() = hashCode().toString()
}
