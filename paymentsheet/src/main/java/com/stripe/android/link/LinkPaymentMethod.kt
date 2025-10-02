package com.stripe.android.link

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.ConsumerPaymentDetails as ConsumerPaymentDetailsModel

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
internal sealed class LinkPaymentMethod(
    open val details: ConsumerPaymentDetailsModel.PaymentDetails,
    open val collectedCvc: String?,
    open val billingPhone: String?
) : Parcelable {

    internal fun readyForConfirmation(): Boolean = when (val currentDetails = details) {
        is ConsumerPaymentDetailsModel.BankAccount -> true
        is ConsumerPaymentDetailsModel.Card -> {
            val cvcReady = !currentDetails.cvcCheck.requiresRecollection || collectedCvc?.isNotEmpty() == true
            !currentDetails.isExpired && cvcReady
        }
    }

    /**
     * The payment method selected by the user within their Link account.
     *
     * @see [com.stripe.android.link.confirmation.LinkConfirmationHandler.confirm]
     * via [com.stripe.android.model.ConsumerPaymentDetails]
     */
    @Parcelize
    internal data class ConsumerPaymentDetails(
        override val details: ConsumerPaymentDetailsModel.PaymentDetails,
        override val collectedCvc: String?,
        override val billingPhone: String?
    ) : LinkPaymentMethod(
        details = details,
        collectedCvc = collectedCvc,
        billingPhone = billingPhone
    )
}
