package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * The mode in which the Link flow is launched.
 */
internal sealed class LinkLaunchMode : Parcelable {
    /**
     * Link is launched with the intent to solely authenticate.
     */
    @Parcelize
    internal data object Authentication : LinkLaunchMode()

    /**
     * Link is launched with the intent to obtain a payment method.
     */
    @Parcelize
    internal data class Payment(
        /**
         * The default payment method to be used for the payment.
         */
        val defaultLinkPayment: ConsumerPaymentDetails.PaymentDetails? = null
    ) : LinkLaunchMode()
}
