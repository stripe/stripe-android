package com.stripe.android

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.parcelize.Parcelize

/**
 * A data class representing the state of the associated [PaymentSession].
 */
@Parcelize
data class PaymentSessionData internal constructor(
    private val isShippingInfoRequired: Boolean,

    private val isShippingMethodRequired: Boolean,

    /**
     * The cart total value, excluding shipping and tax items.
     */
    val cartTotal: Long = 0L,

    /**
     * The current value of the shipping items in the associated [PaymentSession]
     */
    val shippingTotal: Long = 0L,

    /**
     * Where the items being purchased should be shipped.
     */
    val shippingInformation: ShippingInformation? = null,

    /**
     * How the items being purchased should be shipped.
     */
    val shippingMethod: ShippingMethod? = null,

    /**
     * The selected payment method for the associated [PaymentSession].
     */
    val paymentMethod: PaymentMethod? = null,

    /**
     * When `true`, the customer has indicated their intent to pay with Google Pay. Use the
     * [Google Pay API](https://developers.google.com/pay/api/android/overview) to complete
     * payment with Google Pay.
     */
    val useGooglePay: Boolean = false
) : Parcelable {
    internal constructor(config: PaymentSessionConfig) : this(
        isShippingInfoRequired = config.isShippingInfoRequired,
        isShippingMethodRequired = config.isShippingMethodRequired
    )

    /**
     * Whether the payment data is ready for making a charge. This can be used to
     * set a buy button to enabled for prompt a user to fill in more information.
     */
    val isPaymentReadyToCharge: Boolean
        get() =
            (paymentMethod != null || useGooglePay) &&
                (!isShippingInfoRequired || shippingInformation != null) &&
                (!isShippingMethodRequired || shippingMethod != null)
}
