package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

//internal data class LinkPaymentMethodConfirmation(
//    val details: ConsumerPaymentDetails.PaymentDetails,
//    val collectedCvc: String?,
//    val billingPhone: String?,
//) {
//    internal fun readyForConfirmation(): Boolean {
//        return when (val currentDetails = details) {
//            is ConsumerPaymentDetails.BankAccount -> {
//                true
//            }
//            is ConsumerPaymentDetails.Card -> {
//                val cvcReady = !currentDetails.cvcCheck.requiresRecollection || collectedCvc?.isNotEmpty() == true
//                !currentDetails.isExpired && cvcReady
//            }
//        }
//    }
//}

/**
 * Link payment method payload needed to confirm the payment.
 */
// TODO: Replace this
@Parcelize
internal sealed class LinkPaymentMethod(
    open val details: ConsumerPaymentDetails.PaymentDetails,
    open val collectedCvc: String?,
    open val billingPhone: String?
) : Parcelable {

    internal fun readyForConfirmation(): Boolean = when (val currentDetails = details) {
        is ConsumerPaymentDetails.BankAccount -> true
        is ConsumerPaymentDetails.Card -> {
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
    internal data class ConsumerPaymentDetails constructor(
        override val details: ConsumerPaymentDetails.PaymentDetails,
        override val collectedCvc: String?,
        override val billingPhone: String?
    ) : LinkPaymentMethod(
        details = details,
        collectedCvc = collectedCvc,
        billingPhone = billingPhone
    )

    /**
     * The payment method selected by the user within their Link account, including the parameters
     * needed to confirm the Stripe Intent
     *
     * @see [com.stripe.android.link.confirmation.LinkConfirmationHandler.confirm]
     * via [com.stripe.android.link.LinkPaymentDetails]
     *
     */
    // TODO: Deprecate
    @Parcelize
    internal data class LinkPaymentDetails constructor(
        val linkPaymentDetails: com.stripe.android.link.LinkPaymentDetails.New,
        override val collectedCvc: String?,
        override val billingPhone: String?
    ) : LinkPaymentMethod(
        details = linkPaymentDetails.paymentDetails,
        collectedCvc = collectedCvc,
        billingPhone = billingPhone
    )
}
