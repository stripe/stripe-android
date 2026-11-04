package com.stripe.android.utils

import android.graphics.Bitmap
import com.stripe.android.uicore.image.StripeImageLoader

/**
 * [StripeImageLoader] that returns [images] by URL and fails for any other URL.
 */
internal class FakeStripeImageLoader(
    private val images: Map<String, Bitmap> = emptyMap(),
) : StripeImageLoader {
    override suspend fun load(url: String, width: Int, height: Int): Result<Bitmap?> = get(url)

    override suspend fun load(url: String): Result<Bitmap?> = get(url)

    override suspend fun get(url: String): Result<Bitmap?> {
        return images[url]?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No image for $url"))
    }
}
