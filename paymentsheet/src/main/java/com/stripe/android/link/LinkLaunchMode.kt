package com.stripe.android.link

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The mode in which the Link flow is launched.
 */
internal sealed interface LinkLaunchMode : Parcelable {
    /**
     * Link is launched with the intent to select a payment method ready for confirmation.
     */
    @Parcelize
    data object PaymentMethodSelection : LinkLaunchMode

    /**
     * Link is launched in full mode, where the user can authenticate, select a Link payment method and proceed
     * to payment,
     */
    @Parcelize
    data class Full(
        /**
         * The selected Link payment method to be used for confirmation.
         */
        val selectedPayment: LinkPaymentMethod? = null
    ) : LinkLaunchMode
}
