package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * The mode in which the Link flow is launched.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LinkLaunchMode : Parcelable {
    /**
     * Link is launched with the intent to select a payment method ready for confirmation.
     */
    @Parcelize
    data class PaymentMethodSelection(
        /**
         * A previously selected payment that will be preselected at launch
         */
        val selectedPayment: ConsumerPaymentDetails.PaymentDetails?
    ) : LinkLaunchMode

    /**
     * Link is launched in full mode, where the user can authenticate, select a Link payment method and proceed
     * to payment.
     */
    @Parcelize
    data object Full : LinkLaunchMode

    @Parcelize
    data class Confirmation(
        /**
         * The selected Link payment method to be used for confirmation.
         */
        val selectedPayment: LinkPaymentMethod
    ) : LinkLaunchMode

    /**
     * Link is launched in authentication mode, where the user needs to verify their account
     * through the verification process (e.g., OTP, SMS). This mode is focused on authentication
     * only and returns a Link account without any payment processing.
     */
    @Parcelize
    data object Authentication: LinkLaunchMode
}
