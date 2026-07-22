package com.stripe.android.checkout

import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko

@Poko
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentOptionDisplayData internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val imageLoader: suspend () -> Drawable,
    /**
     * A user facing string representing the payment method; e.g. "Google Pay" or "···· 4242" for a card.
     */
    val label: String,
    /**
     * A string representation of the customer's desired payment method:
     * - If this is a Stripe payment method, see
     *      https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for possible values.
     * - If this is an external payment method, see
     *      https://docs.stripe.com/payments/mobile/external-payment-methods?platform=android
     *      for possible values.
     * - If this is Google Pay, the value is "google_pay".
     */
    val paymentMethodType: String,
    /**
     * If you set [CheckoutController.Configuration.embeddedViewDisplaysMandateText] to `false`, this text must be
     * displayed to the customer near your "Buy" button to comply with regulations.
     */
    val mandateText: AnnotatedString?,
) {
    private val iconDrawable: Drawable by lazy {
        DelegateDrawable(imageLoader)
    }

    /**
     * An image representing a payment method; e.g. the Google Pay logo or a VISA logo.
     */
    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(iconDrawable)
}
