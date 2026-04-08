package com.stripe.android.paymentsheet

import android.content.Context
import android.util.TypedValue
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.image.ImageOptimizer
import javax.inject.Inject

internal interface PaymentOptionCardArtProvider {
    operator fun invoke(cardArt: PaymentMethod.Card.CardArt): String?
}

internal class DefaultPaymentOptionCardArtProvider @Inject constructor(
    private val context: Context,
    private val imageOptimizer: ImageOptimizer
) : PaymentOptionCardArtProvider {
    override operator fun invoke(cardArt: PaymentMethod.Card.CardArt): String? {
        val cardArtUrl = cardArt.artImage?.url ?: return null
        val pxSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            40f,
            context.resources.displayMetrics
        ).toInt()
        return imageOptimizer.optimize(cardArtUrl, pxSize)
    }

}
