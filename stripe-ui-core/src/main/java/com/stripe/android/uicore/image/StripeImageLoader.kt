package com.stripe.android.uicore.image

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import kotlinx.coroutines.sync.Mutex
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
class StripeImageLoader() {
    private val imageLoadMutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun load(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap?> = Result.success(null)

    suspend fun load(url: String): Result<Bitmap?> = Result.success(null)

    private fun loadFromMemory(url: String): Result<Bitmap>? {
        return null

    }

    private fun loadFromDisk(url: String): Result<Bitmap>? = null


    @WorkerThread
    private suspend fun loadFromNetwork(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap?> =          Result.success(null)


    @WorkerThread
    private suspend fun loadFromNetwork(url: String): Result<Bitmap?> =          Result.success(null)


    private companion object {
        private const val TAG = "StripeImageLoader"
    }
}

private fun Context.isDebuggable(): Boolean =
    (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))