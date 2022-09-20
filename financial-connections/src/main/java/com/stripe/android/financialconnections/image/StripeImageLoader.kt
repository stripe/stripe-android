package com.stripe.android.financialconnections.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.InputStream
import java.net.URL

/**
 * Image loader that fetches images from memory, disk or network and runs
 * cache policy accordingly.
 *
 * [StripeImageLoader] is stateful as it holds the memoryCache instance. For
 * memory cache to work the image loader instance needs to be shared.
 */
internal class StripeImageLoader(
    lifecycleOwner: LifecycleOwner,
    context: Context,
) : DefaultLifecycleObserver {
    private var currentInputStream: InputStream? = null

    private val diskCache = ImageLruDiskCache(
        context = context,
        uniqueName = "financial_connections_image_cache"
    )
    private val memoryCache = ImageLruMemoryCache()

    init {
        registerLifecycleObserver(lifecycleOwner)
    }

    private fun registerLifecycleObserver(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cancel()
                super.onDestroy(owner)
            }
        })
    }

    @WorkerThread
    fun load(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap> {
        return loadFromMemory(url)
            ?: loadFromDisk(url)
            ?: loadFromNetwork(url, width, height)
                .onSuccess {
                    debug("Image loaded from internet")
                    diskCache.put(url, it)
                    memoryCache.put(url, it)
                }
                .onFailure {
                    debug("Could not load image from network")
                }
    }

    private fun loadFromDisk(url: String): Result<Bitmap>? {
        return diskCache.getBitmap(url)
            .also {
                if (it != null) {
                    debug("Image loaded from disk cache")
                } else {
                    debug("Image not found on disk cache")
                }
            }
            ?.let {
                memoryCache.put(url, it)
                Result.success(it)
            }
    }

    private fun loadFromMemory(url: String): Result<Bitmap>? {
        return memoryCache.getBitmap(url)
            .also {
                if (it != null) {
                    debug("Image loaded from memory cache")
                } else {
                    debug("Image not found on memory cache")
                }
            }
            ?.let {
                diskCache.put(url, it)
                Result.success(it)
            }
    }

    private fun loadFromNetwork(
        url: String,
        width: Int,
        height: Int
    ): Result<Bitmap> = kotlin.runCatching {
        BitmapFactory.Options().run {
            // First decode with inJustDecodeBounds=true to check dimensions
            inJustDecodeBounds = true
            BitmapFactory.decodeStream(
                URL(url).openStream().also { stream -> currentInputStream = stream },
                null,
                this
            )
            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, width, height)
            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            BitmapFactory.decodeStream(
                URL(url).openStream().also { stream -> currentInputStream = stream },
                null,
                this
            )!!
        }
    }

    fun cancel() {
        currentInputStream?.close()
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

    fun debug(message: String) {
        Log.d("StripeImage", message)
    }
}
