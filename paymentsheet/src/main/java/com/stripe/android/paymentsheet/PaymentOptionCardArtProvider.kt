package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.image.ImageOptimizer
import javax.inject.Inject

internal fun interface PaymentOptionCardArtProvider {
    operator fun invoke(cardArt: PaymentMethod.Card.CardArt): String?
}

internal class DefaultPaymentOptionCardArtProvider @Inject constructor(
    private val imageOptimizer: ImageOptimizer
) : PaymentOptionCardArtProvider {
    override operator fun invoke(cardArt: PaymentMethod.Card.CardArt): String? {
        val cardArtUrl = cardArt.artImage?.url ?: return null
        return imageOptimizer.optimize(cardArtUrl, CARD_ART_WIDTH_PX)
    }

    companion object {
        internal const val CARD_ART_WIDTH_PX = 44
    }
}
