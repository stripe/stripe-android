package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.image.rememberDrawablePainter

/**
 * The customer's selected payment option.
 */
data class PaymentOption internal constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @Deprecated("Please use icon() instead.")
    val drawableResourceId: Int,
    /**
     * A label that describes the payment option.
     *
     * For example, "···· 4242" for a Visa ending in 4242.
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
    private val _shippingDetails: AddressDetails?,

    private val imageLoader: suspend () -> Drawable,
) {

    /**
     * A shipping address that the user provided during checkout.
     */
    @ShippingDetailsInPaymentOptionPreview
    val shippingDetails: AddressDetails?
        get() = _shippingDetails

    @Deprecated("Not intended for public use.")
    constructor(
        @DrawableRes
        drawableResourceId: Int,
        label: String
    ) : this(
        drawableResourceId = drawableResourceId,
        label = label,
        paymentMethodType = "unsupportedInitializationType",
        _shippingDetails = null,
        imageLoader = errorImageLoader,
    )

    /**
     * A [Painter] to draw the icon associated with this [PaymentOption].
     */
    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(icon())

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    fun icon(): Drawable {
        return DelegateDrawable(
            imageLoader = imageLoader,
        )
    }
}

private val errorImageLoader: suspend () -> Drawable = {
    throw IllegalStateException("Must pass in an image loader to use icon() or iconPainter.")
}
