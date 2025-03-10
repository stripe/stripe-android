package com.stripe.android.uicore.image

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
    private val logger: Logger = Logger.getInstance(context.isDebuggable()),
    private val memoryCache: ImageLruMemoryCache? = ImageLruMemoryCache(),
    private val networkImageDecoder: NetworkImageDecoder = NetworkImageDecoder(),
    private val diskCache: ImageLruDiskCache? = ImageLruDiskCache(
        context = context,
        cacheFolder = "stripe_image_cache"
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
    ): Result<Bitmap?> = withContext(Dispatchers.IO) {
        withMutexByUrlLock(url) {
            loadFromMemory(url) ?: loadFromDisk(url) ?: loadFromNetwork(url, width, height)
        }
    }

    suspend fun load(
        url: String
    ): Result<Bitmap?> = withContext(Dispatchers.IO) {
        withMutexByUrlLock(url) {
            loadFromMemory(url) ?: loadFromDisk(url) ?: loadFromNetwork(url)
        }
    }

    private fun loadFromMemory(url: String): Result<Bitmap>? {
        return memoryCache?.get(url)
            .also {
                if (it != null) {
                    debug("Image loaded from memory cache")
                } else {
                    debug("Image not found on memory cache")
                }
            }
            ?.let {
                diskCache?.put(url, it)
                Result.success(it.bitmap)
            }
    }

    private fun loadFromDisk(url: String): Result<Bitmap>? = diskCache?.get(url)
        .also {
            if (it != null) {
                debug("Image loaded from disk cache")
            } else {
                debug("Image not found on disk cache")
            }
        }
        ?.let {
            memoryCache?.put(url, it)
            Result.success(it.bitmap)
        }

    @WorkerThread
    private suspend fun loadFromNetwork(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap?> = kotlin.runCatching {
        debug("Image $url loading from internet ($width x $height)")
        networkImageDecoder.decode(url, width, height)?.let { image ->
            diskCache?.put(url, image)
            memoryCache?.put(url, image)
            image.bitmap
        }
    }.onFailure { logger.error("$TAG: Could not load image from network", it) }

    @WorkerThread
    private suspend fun loadFromNetwork(
        url: String
    ): Result<Bitmap?> = kotlin.runCatching {
        debug("Image $url loading from internet")
        networkImageDecoder.decode(url)?.let { image ->
            diskCache?.put(url, image)
            memoryCache?.put(url, image)
            image.bitmap
        }
    }.onFailure { logger.error("$TAG: Could not load image from network", it) }

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

private fun Context.isDebuggable(): Boolean =
    (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))
