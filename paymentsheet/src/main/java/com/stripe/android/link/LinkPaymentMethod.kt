package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
internal sealed class LinkPaymentMethod(
    open val collectedCvc: String?,
) : Parcelable {

    internal abstract val details: ConsumerPaymentDetails.PaymentDetails

    internal fun readyForConfirmation(): Boolean = when (val currentDetails = details) {
        is ConsumerPaymentDetails.BankAccount -> true
        is ConsumerPaymentDetails.Card -> {
            val cvcReady = !currentDetails.cvcCheck.requiresRecollection || collectedCvc?.isNotEmpty() == true
            !currentDetails.isExpired && cvcReady
        }
        is ConsumerPaymentDetails.Passthrough -> true
    }

    @Parcelize
    internal data class Consumer(
        val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        override val collectedCvc: String?
    ) : LinkPaymentMethod(collectedCvc) {
        override val details
            get() = paymentDetails
    }

    @Parcelize
    internal data class Link(
        val linkPaymentDetails: LinkPaymentDetails,
        override val collectedCvc: String?
    ) : LinkPaymentMethod(collectedCvc) {

        override val details
            get() = linkPaymentDetails.paymentDetails
    }
}
