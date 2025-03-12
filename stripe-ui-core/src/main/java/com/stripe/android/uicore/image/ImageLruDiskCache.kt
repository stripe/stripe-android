@file:Suppress("MagicNumber")

package com.stripe.android.uicore.image

import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.RestrictTo
import com.jakewharton.disklrucache.DiskLruCache
import com.stripe.android.uicore.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Implementation of disk cache based on [DiskLruCache].
 *
 * @see [DiskLruCache]

 * @param cacheFolder name of the folder that will store the images of this cache.
 *        It will create a cache if none exists there.
 * @param maxSizeBytes the maximum number of bytes this cache should use to store
 * @throws IOException if reading or writing the cache directory fails
 **/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ImageLruDiskCache(
    context: Context,
    cacheFolder: String,
    maxSizeBytes: Long = 10L * 1024 * 1024, // 10MB
) {
    private val diskLruCache: DiskLruCache? by lazy {
        try {
            DiskLruCache.open(
                /* directory = */
                getDiskCacheDir(context, cacheFolder),
                /* appVersion = */
                APP_VERSION,
                /* valueCount = */
                VALUE_COUNT,
                /* maxSize = */
                maxSizeBytes
            )
        } catch (e: IOException) {
            Log.e(TAG, "error opening cache", e)
            null
        }
    }

    fun put(key: String, image: LoadedImage) {
        var editor: DiskLruCache.Editor? = null
        val hashedKey = key.toKey()
        if (containsKey(key)) {
            debug("Image already cached")
        } else {
            try {
                editor = diskLruCache?.edit(hashedKey)
                if (editor == null) return
                val compressFormat = image.contentType.toCompressFormat()
                if (
                    writeImageToFile(
                        image = image,
                        editor = editor,
                        compressFormat = compressFormat,
                        compressQuality = compressFormat.quality()
                    )
                ) {
                    diskLruCache?.flush()
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
    }

    private fun LoadedImage.ContentType.toCompressFormat() = when (this) {
        LoadedImage.ContentType.Jpeg -> CompressFormat.JPEG
        LoadedImage.ContentType.Png -> CompressFormat.PNG
        LoadedImage.ContentType.Webp -> CompressFormat.WEBP
        else -> throw IllegalArgumentException("Unexpected image type: $value")
    }

    private fun CompressFormat.quality(): Int = when (this) {
        CompressFormat.JPEG -> JPEG_COMPRESS_QUALITY
        CompressFormat.PNG -> PNG_COMPRESS_QUALITY
        CompressFormat.WEBP -> WEBP_COMPRESS_QUALITY
        else -> throw IllegalArgumentException("Unexpected compress format: $this")
    }

    fun get(key: String): LoadedImage? {
        var image: LoadedImage? = null
        var snapshot: DiskLruCache.Snapshot? = null
        val hashedKey = key.toKey()
        try {
            snapshot = diskLruCache?.get(hashedKey)
            if (snapshot == null) {
                debug(
                    "image not in cache: $hashedKey"
                )
                return null
            }
            val inputStream: InputStream = snapshot.getInputStream(0)
            val contentType = snapshot.getString(1)
            val buffIn = BufferedInputStream(inputStream, IO_BUFFER_SIZE)
            val bitmap = BitmapFactory.decodeStream(buffIn)

            image = LoadedImage(
                contentType = contentType,
                bitmap = bitmap,
            )
        } catch (e: IOException) {
            Log.e(TAG, "error getting bitmap from cache", e)
        } finally {
            snapshot?.close()
        }
        debug(
            if (image == null) {
                "image not in cache: $hashedKey"
            } else {
                "image read from disk $hashedKey"
            }
        )
        return image
    }

    fun containsKey(key: String): Boolean {
        var contained = false
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = diskLruCache?.get(key.toKey())
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
            diskLruCache?.delete()
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
    private fun writeImageToFile(
        image: LoadedImage,
        editor: DiskLruCache.Editor,
        compressFormat: CompressFormat,
        compressQuality: Int
    ): Boolean {
        var out: OutputStream? = null
        return try {
            out = BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)
            editor.set(1, image.contentType.value)
            image.bitmap.compress(compressFormat, compressQuality, out)
        } finally {
            out?.close()
        }
    }

    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath: String = context.cacheDir.path
        return File(cachePath + File.separator + uniqueName)
    }

    private companion object {
        private const val TAG = "stripe_image_disk_cache"
        private const val APP_VERSION = 2
        private const val VALUE_COUNT = 2
        private const val IO_BUFFER_SIZE = 8 * 1024

        private const val PNG_COMPRESS_QUALITY = 100
        private const val JPEG_COMPRESS_QUALITY = 80
        private const val WEBP_COMPRESS_QUALITY = 80
    }
}
