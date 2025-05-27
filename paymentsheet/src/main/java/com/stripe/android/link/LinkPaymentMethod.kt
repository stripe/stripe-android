package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
internal sealed class LinkPaymentMethod(
    open val details: ConsumerPaymentDetails.PaymentDetails,
    open val collectedCvc: String?,
) : Parcelable {

    internal fun readyForConfirmation(): Boolean = when (val currentDetails = details) {
        is ConsumerPaymentDetails.BankAccount -> true
        is ConsumerPaymentDetails.Card -> {
            val cvcReady = !currentDetails.cvcCheck.requiresRecollection || collectedCvc?.isNotEmpty() == true
            !currentDetails.isExpired && cvcReady
        }
        is ConsumerPaymentDetails.Passthrough -> true
    }

    /**
     * The payment method selected by the user within their Link account.
     *
     * @see [com.stripe.android.link.confirmation.LinkConfirmationHandler.confirm]
     * via [com.stripe.android.model.ConsumerPaymentDetails]
     */
    @Parcelize
    internal data class ConsumerPaymentDetails(
        override val details: ConsumerPaymentDetails.PaymentDetails,
        override val collectedCvc: String?
    ) : LinkPaymentMethod(details, collectedCvc)

    /**
     * The payment method selected by the user within their Link account, including the parameters
     * needed to confirm the Stripe Intent
     *
     * @see [com.stripe.android.link.confirmation.LinkConfirmationHandler.confirm]
     * via [com.stripe.android.link.LinkPaymentDetails]
     *
     */
    @Parcelize
    internal data class LinkPaymentDetails(
        val linkPaymentDetails: com.stripe.android.link.LinkPaymentDetails,
        override val collectedCvc: String?
    ) : LinkPaymentMethod(linkPaymentDetails.paymentDetails, collectedCvc)
}
