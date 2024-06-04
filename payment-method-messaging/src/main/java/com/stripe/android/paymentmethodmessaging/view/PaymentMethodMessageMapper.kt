package com.stripe.android.paymentmethodmessaging.view

import android.graphics.Bitmap
import android.text.style.ImageSpan
import androidx.core.text.HtmlCompat
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class PaymentMethodMessageMapper @Inject constructor(
    private val config: PaymentMethodMessagingView.Configuration,
    private val imageLoader: StripeImageLoader
) {
    fun mapAsync(
        scope: CoroutineScope,
        message: PaymentMethodMessage,
        imageGetter: suspend () -> Map<String, Bitmap> = suspend {
            message.displayHtml.getBitmapsAsync()
        }
    ): Deferred<PaymentMethodMessagingData> {
        return scope.async {
            PaymentMethodMessagingData(
                message = message,
                images = imageGetter(),
                config = config
            )
        }
    }

    private suspend fun String.getBitmapsAsync(): Map<String, Bitmap> = coroutineScope {
        val spanned = HtmlCompat.fromHtml(this@getBitmapsAsync, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val images = spanned
            .getSpans(0, spanned.length, Any::class.java)
            .filterIsInstance<ImageSpan>()
            .map { it.source!! }

        val deferred = images.map { url ->
            async {
                Pair(url, imageLoader.load(url).getOrNull())
            }
        }

        val bitmaps = deferred.awaitAll()

        bitmaps.mapNotNull { pair ->
            pair.second?.let { bitmap ->
                Pair(pair.first, bitmap)
            }
        }.toMap()
    }
}
