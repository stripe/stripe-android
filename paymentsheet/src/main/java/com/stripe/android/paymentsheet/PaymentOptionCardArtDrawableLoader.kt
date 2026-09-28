package com.stripe.android.paymentsheet

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal fun interface PaymentOptionCardArtDrawableLoader {
    suspend fun load(selection: PaymentSelection): Drawable?
}

internal class DefaultPaymentOptionCardArtDrawableLoader @Inject constructor(
    private val paymentOptionCardArtProvider: PaymentOptionCardArtProvider,
    private val imageLoader: StripeImageLoader,
    private val errorReporter: ErrorReporter,
    private val context: Context,
) : PaymentOptionCardArtDrawableLoader {
    override suspend fun load(selection: PaymentSelection): Drawable? {
        val cardArt = (selection as? PaymentSelection.Saved)?.paymentMethod?.card?.cardArt ?: return null
        val url = paymentOptionCardArtProvider(cardArt) ?: return null
        return imageLoader.get(url)
            .mapCatching { bitmap -> bitmap?.toDrawable(context.resources) }
            .onFailure { error ->
                errorReporter.report(
                    errorEvent = ExpectedErrorEvent.PAYMENT_OPTION_CARD_ART_LOAD_FAILURE,
                    stripeException = StripeException.create(error),
                )
            }
            .getOrNull()
    }
}
