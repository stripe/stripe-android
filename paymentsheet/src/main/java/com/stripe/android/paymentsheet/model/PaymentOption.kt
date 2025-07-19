package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko

/**
 * The customer's selected payment option.
 */
data class PaymentOption internal constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    internal val drawableResourceId: Int,
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

    /**
     * The billing details associated with the customer's desired payment method.
     */
    val billingDetails: PaymentSheet.BillingDetails?,
    private val _shippingDetails: AddressDetails?,
    private val _labels: Labels,

    private val imageLoader: suspend () -> Drawable,
) {

    @Poko
    class Labels internal constructor(
        /**
         * Primary label for the payment option. This will primarily describe the type of the payment option being used.
         * For cards, this could be `Mastercard`, 'Visa', or others. For other payment methods, this is typically the
         * payment method name.
         */
        val label: String,
        /**
         * Secondary optional label for the payment option. This will primarily describe any expanded details about the
         * payment option such as the last four digits of a card or bank account.
         */
        val sublabel: String? = null,
    )

    /**
     * Labels containing additional information about the payment option.
     */
    @ExtendedLabelsInPaymentOptionPreview
    val labels: Labels
        get() = _labels

    /**
     * A shipping address that the user provided during checkout.
     */
    @ShippingDetailsInPaymentOptionPreview
    val shippingDetails: AddressDetails?
        get() = _shippingDetails

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
