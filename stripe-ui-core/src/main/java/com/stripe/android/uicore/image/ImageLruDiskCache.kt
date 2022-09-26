@file:Suppress("MagicNumber")

package com.stripe.android.uicore.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.util.Log
import com.stripe.android.uicore.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class ImageLruDiskCache(
    context: Context,
    uniqueName: String,
    maxSizeBytes: Long = 10L * 1024 * 1024, // 10MB
    private val compressFormat: CompressFormat = CompressFormat.JPEG,
    private val mCompressQuality: Int = 70,
) {
    private lateinit var diskLruCache: DiskLruCache

    init {
        try {
            diskLruCache = DiskLruCache.open(
                /* directory = */ getDiskCacheDir(context, uniqueName),
                /* appVersion = */ APP_VERSION,
                /* valueCount = */ VALUE_COUNT,
                /* maxSize = */ maxSizeBytes
            )
        } catch (e: IOException) {
            Log.e(TAG, "error opening cache", e)
        }
    }

    fun put(key: String, data: Bitmap) {
        var editor: DiskLruCache.Editor? = null
        val hashedKey = key.toKey()
        if (containsKey(key)) {
            debug("Image already cached")
        } else try {
            editor = diskLruCache.edit(hashedKey)
            if (editor == null) return
            if (writeBitmapToFile(data, editor)) {
                diskLruCache.flush()
                editor.commit()
                debug("image put on disk cache $hashedKey")
            } else {
                editor.abort()
                Log.e(TAG, "ERROR on: image put on disk cache $hashedKey")
            }
        } catch (e: IOException) {
            Log.e(TAG, "ERROR on: image put on disk cache $hashedKey")
            kotlin.runCatching { editor?.abort() }
        }
    }

    fun getBitmap(key: String): Bitmap? {
        var bitmap: Bitmap? = null
        var snapshot: DiskLruCache.Snapshot? = null
        val hashedKey = key.toKey()
        try {
            snapshot = diskLruCache.get(hashedKey)
            if (snapshot == null) {
                debug(
                    "image not in cache: $hashedKey"
                )
                return null
            }
            val inputStream: InputStream = snapshot.getInputStream(0)
            val buffIn = BufferedInputStream(inputStream, IO_BUFFER_SIZE)
            bitmap = BitmapFactory.decodeStream(buffIn)
        } catch (e: IOException) {
            Log.e(TAG, "error getting bitmap from cache", e)
        } finally {
            snapshot?.close()
        }
        debug(
            if (bitmap == null) {
                "image not in cache: $hashedKey"
            } else {
                "image read from disk $hashedKey"
            }
        )
        return bitmap
    }

    fun containsKey(key: String): Boolean {
        var contained = false
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = diskLruCache.get(key.toKey())
            contained = snapshot != null
        } catch (e: IOException) {
            Log.e(TAG, "error reading from cache", e)
        } finally {
            snapshot?.close()
        }
        return contained
    }

    fun clearCache() {
        debug("disk cache CLEARED")
        try {
            diskLruCache.delete()
        } catch (e: IOException) {
            Log.e(TAG, "error clearing cache", e)
        }
    }

    private fun debug(s: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, s)
    }

    /**
     * [DiskLruCache] just accepts keys matching [a-z0-9_-]{1,64}. Keys (image urls)
     * are hashed to ensure pattern match.
     */
    private fun String.toKey(): String = hashCode().toString()

    @Throws(IOException::class, FileNotFoundException::class)
    private fun writeBitmapToFile(bitmap: Bitmap, editor: DiskLruCache.Editor): Boolean {
        var out: OutputStream? = null
        return try {
            out = BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)
            bitmap.compress(compressFormat, mCompressQuality, out)
        } finally {
            out?.close()
        }
    }

    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath: String = context.cacheDir.path
        return File(cachePath + File.separator + uniqueName)
    }
    companion object {
        private const val TAG = "stripe_image_disk_cache"
        private const val APP_VERSION = 1
        private const val VALUE_COUNT = 1
        private const val IO_BUFFER_SIZE = 8 * 1024
    }
}
