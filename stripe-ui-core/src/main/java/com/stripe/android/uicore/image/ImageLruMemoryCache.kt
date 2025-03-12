package com.stripe.android.uicore.image

import android.util.LruCache
import androidx.annotation.RestrictTo

/**
 * Implementation of in-memory cache based on [LruCache].
 *
 * @see [LruCache]
 * @param maxSize maximum sum of the sizes of the entries in this cache.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("MagicNumber")
class ImageLruMemoryCache(
    // Use 1/8th of the available memory for this memory cache.
    val maxSize: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
) {

    @Suppress("MagicNumber")
    private val lruCache = object : LruCache<String, LoadedImage>(maxSize) {
        override fun sizeOf(key: String, image: LoadedImage): Int {
            return image.bitmap.byteCount / 1024
        }
    }

    fun put(key: String, image: LoadedImage) {
        synchronized(this) {
            if (lruCache.get(key.toKey()) == null) {
                lruCache.put(key.toKey(), image)
            }
        }
    }

    fun get(key: String): LoadedImage? {
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
