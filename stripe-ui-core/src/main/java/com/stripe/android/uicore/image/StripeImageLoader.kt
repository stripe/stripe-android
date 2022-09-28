package com.stripe.android.uicore.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Image loader that fetches images from memory, disk or network and runs
 * cache policy accordingly.
 *
 * [StripeImageLoader] is stateful as it holds the memoryCache instance. For
 * memory cache to work the image loader instance needs to be shared.
 *
 * @param memoryCache, memory cache to be used, or null if no memory cache is desired.
 * @param diskCache, memory cache to be used, or null if no memory cache is desired.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeImageLoader(
    context: Context,
    private val logger: Logger,
    private val memoryCache: ImageLruMemoryCache? = ImageLruMemoryCache(),
    private val diskCache: ImageLruDiskCache? = ImageLruDiskCache(
        context = context,
        uniqueName = "stripe_image_cache"
    ),
) {

    private val imageLoadMutexes = ConcurrentHashMap<String, Mutex>()

    /**
     * loads the given [url] with the associated [width]x[height].
     *
     * If the same [url] is being loaded concurrently, function will be suspended until
     * the original load completes.
     */
    suspend fun load(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        withMutexByUrlLock(url) {
            loadFromMemory(url) ?: loadFromDisk(url) ?: loadFromNetwork(url, width, height)
        }
    }

    private fun loadFromMemory(url: String): Result<Bitmap>? {
        return memoryCache?.getBitmap(url)
            .also {
                if (it != null) {
                    debug("Image loaded from memory cache")
                } else {
                    debug("Image not found on memory cache")
                }
            }
            ?.let {
                diskCache?.put(url, it)
                Result.success(it)
            }
    }

    private fun loadFromDisk(url: String): Result<Bitmap>? {
        return diskCache?.getBitmap(url)
            .also {
                if (it != null) {
                    debug("Image loaded from disk cache")
                } else {
                    debug("Image not found on disk cache")
                }
            }
            ?.let {
                memoryCache?.put(url, it)
                Result.success(it)
            }
    }

    @WorkerThread
    private suspend fun loadFromNetwork(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap> = kotlin.runCatching {
        debug("Image $url loading from internet")
        val bitmap = BitmapFactory.Options().run {
            // First decode with inJustDecodeBounds=true to check dimensions
            inJustDecodeBounds = true
            decodeStream(url)
            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, width, height)
            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            decodeStream(url)
        }!!
        diskCache?.put(url, bitmap)
        memoryCache?.put(url, bitmap)
        bitmap
    }.onFailure { logger.error("$TAG: Could not load image from network", it) }

    private suspend fun BitmapFactory.Options.decodeStream(
        url: String
    ): Bitmap? = suspendCancellableCoroutine { cont ->
        kotlin.runCatching {
            URL(url).openStream()
                .also { stream -> cont.invokeOnCancellation { stream.close() } }
                .use { BitmapFactory.decodeStream(it, null, this) }
        }.fold(
            onSuccess = { cont.resume(it) },
            onFailure = { cont.resumeWithException(it) }
        )
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Runs the specified [action] within a locked mutex keyed by the passed url.
     */
    private suspend fun <T> withMutexByUrlLock(url: String, action: suspend () -> T): T {
        return imageLoadMutexes.getOrPut(url) { Mutex() }.withLock {
            action()
        }.also {
            imageLoadMutexes.remove(url)
        }
    }

    private fun debug(message: String) {
        logger.debug("$TAG: $message")
    }

    private companion object {
        private const val TAG = "StripeImageLoader"
    }
}
