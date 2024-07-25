package com.stripe.android.stripe3ds2.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transaction.StripeHttpClient
import com.stripe.android.stripe3ds2.utils.ImageCache
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class ImageRepository constructor(
    private val workContext: CoroutineContext,
    private val imageCache: ImageCache,
    private val imageSupplier: ImageSupplier
) {
    constructor(
        errorReporter: ErrorReporter,
        workContext: CoroutineContext
    ) : this(
        workContext,
        ImageCache.Default,
        ImageSupplier.Default(errorReporter, workContext)
    )

    /**
     * If [imageUrl] is non-null, will first attempt to retrieve the image locally.
     * If not available, will fetch the image remotely.
     *
     * @return a [Bitmap] if one was successfully retrieved; otherwise, `null`
     */
    internal suspend fun getImage(imageUrl: String?): Bitmap? = withContext(workContext) {
        imageUrl?.let {
            getLocalImage(imageUrl) ?: getRemoteImage(imageUrl).also { cacheImage(imageUrl, it) }
        }
    }

    private fun getLocalImage(imageUrl: String) = imageCache[imageUrl]

    private suspend fun getRemoteImage(imageUrl: String) = imageSupplier.getBitmap(imageUrl)

    private fun cacheImage(imageUrl: String, image: Bitmap?) {
        if (image != null) {
            imageCache[imageUrl] = image
        }
    }

    interface ImageSupplier {
        suspend fun getBitmap(url: String): Bitmap?

        class Default(
            private val errorReporter: ErrorReporter,
            private val workContext: CoroutineContext
        ) : ImageSupplier {
            override suspend fun getBitmap(url: String): Bitmap? {
                return runCatching {
                    StripeHttpClient(
                        url,
                        errorReporter = errorReporter,
                        workContext = workContext
                    ).doGetRequest()?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.onFailure {
                    errorReporter.reportError(
                        RuntimeException("Could not get bitmap from url: $url.", it)
                    )
                }.getOrNull()
            }
        }
    }
}
