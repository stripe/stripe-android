package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * The mode in which the Link flow is launched.
 */
internal sealed interface LinkLaunchMode : Parcelable {
    /**
     * Link is launched with the intent to select a payment method ready for confirmation.
     */
    @Parcelize
    data class PaymentMethodSelection(
        /**
         * A previously selected payment that will be preselected at launch
         */
        val selectedPayment: ConsumerPaymentDetails.PaymentDetails?,

        /**
         * If true, shares the payment method after creating it in passthrough mode.
         */
        val shareAfterCreatingInPassthroughMode: Boolean = true,
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
     * Link is launched with the intent to authenticate the user only.
     * The flow will close after successful authentication instead of continuing to payment selection.
     */
    @Parcelize
    data class Authentication(
        val existingOnly: Boolean = false,
    ) : LinkLaunchMode
}
