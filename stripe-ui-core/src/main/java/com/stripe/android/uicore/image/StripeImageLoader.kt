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
import kotlin.coroutines.CoroutineContext

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
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) {
    private val imageLoadMutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun load(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap?> = withContext(coroutineContext) {
        withMutexByUrlLock(url) {
            loadFromMemory(url) ?: runCatching {
                loadFromDisk(url) ?: loadFromNetwork(url, width, height)
            }.getOrElse { throwable ->
                logger.error("$TAG: Failed to load image from disk or network", throwable)
                Result.failure(throwable)
            }
        }
    }

    suspend fun load(url: String): Result<Bitmap?> = withContext(coroutineContext) {
        withMutexByUrlLock(url) {
            loadFromMemory(url) ?: runCatching {
                loadFromDisk(url) ?: loadFromNetwork(url)
            }.getOrElse { throwable ->
                logger.error("$TAG: Failed to load image from disk or network", throwable)
                Result.failure(throwable)
            }
        }
    }

    private fun loadFromMemory(url: String): Result<Bitmap>? {
        return memoryCache?.getBitmap(url)
            .also {
                if (it != null) {
                    logger.debug("$TAG: Image loaded from memory cache for URL: $url")
                } else {
                    logger.debug("$TAG: Image not found in memory cache for URL: $url")
                }
            }
            ?.let {
                Result.success(it)
            }
    }

    private fun loadFromDisk(url: String): Result<Bitmap>? = kotlin.runCatching {
        diskCache?.getBitmap(url)
    }.onSuccess {
        if (it != null) {
            logger.debug("$TAG: Image loaded from disk cache for URL: $url")
            memoryCache?.put(url, it)
        } else {
            logger.debug("$TAG: Image not found in disk cache for URL: $url")
        }
    }.getOrNull()?.let {
        Result.success(it)
    }

    @WorkerThread
    private suspend fun loadFromNetwork(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap?> = kotlin.runCatching {
        logger.debug("$TAG: Loading image from network URL: $url ($width x $height)")
        networkImageDecoder.decode(url, width, height)?.also { bitmap ->
            diskCache?.put(url, bitmap)
            memoryCache?.put(url, bitmap)
        }
    }.onFailure {
        logger.error("$TAG: Failed to load image from network URL: $url", it)
    }

    @WorkerThread
    private suspend fun loadFromNetwork(url: String): Result<Bitmap?> = kotlin.runCatching {
        logger.debug("$TAG: Loading image from network URL: $url")
        networkImageDecoder.decode(url)?.also { bitmap ->
            diskCache?.put(url, bitmap)
            memoryCache?.put(url, bitmap)
        }
    }.onFailure {
        logger.error("$TAG: Failed to load image from network URL: $url", it)
    }

    private suspend fun <T> withMutexByUrlLock(url: String, action: suspend () -> T): T {
        return imageLoadMutexes.getOrPut(url) { Mutex() }.withLock {
            action()
        }.also {
            imageLoadMutexes.remove(url)
        }
    }

    private companion object {
        private const val TAG = "StripeImageLoader"
    }
}

private fun Context.isDebuggable(): Boolean =
    (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))