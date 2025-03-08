package com.stripe.android.uicore.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.RestrictTo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NetworkImageDecoder {

    /**
     *  Fetches a [url] from network and decodes them to a [Bitmap] with the specified width and
     *  height.
     */
    suspend fun decode(
        url: String,
        width: Int,
        height: Int
    ): LoadedImage? {
        return BitmapFactory.Options().run {
            // First decode with inJustDecodeBounds=true to check dimensions
            inJustDecodeBounds = true
            decodeStream(url)
            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, width, height)
            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            decodeStream(url)
        }
    }

    suspend fun decode(
        url: String
    ): LoadedImage? {
        return BitmapFactory.Options().run {
            decodeStream(url)
        }
    }

    private suspend fun BitmapFactory.Options.decodeStream(
        url: String
    ): LoadedImage? = suspendCancellableCoroutine { cont ->
        kotlin.runCatching {
            URL(url).stream()
                .also { stream -> cont.invokeOnCancellation { runCatching { stream.close() } } }
                .use { BitmapFactory.decodeStream(it, null, this) }
                ?.let { bitmap ->
                    LoadedImage(
                        contentType = outMimeType,
                        bitmap = bitmap,
                    )
                }
        }.fold(
            onSuccess = { cont.resume(it) },
            onFailure = { cont.resumeWithException(it) }
        )
    }

    private fun URL.stream(): InputStream {
        val con: URLConnection = openConnection()
        con.connectTimeout = IMAGE_STREAM_TIMEOUT
        con.readTimeout = IMAGE_STREAM_TIMEOUT
        return con.getInputStream()
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

    private companion object {
        const val IMAGE_STREAM_TIMEOUT = 10_000
    }
}
