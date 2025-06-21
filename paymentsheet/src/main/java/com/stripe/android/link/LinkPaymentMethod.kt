package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.ConsumerPaymentDetails as ConsumerPaymentDetailsModel

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LinkPaymentMethod(
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
        is ConsumerPaymentDetailsModel.Passthrough -> true
    }

    /**
     * The payment method selected by the user within their Link account.
     *
     * @see [com.stripe.android.link.confirmation.LinkConfirmationHandler.confirm]
     * via [com.stripe.android.model.ConsumerPaymentDetails]
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ConsumerPaymentDetails(
        override val details: ConsumerPaymentDetailsModel.PaymentDetails,
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
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class LinkPaymentDetails(
        val linkPaymentDetails: com.stripe.android.link.LinkPaymentDetails,
        override val collectedCvc: String?,
        override val billingPhone: String?
    ) : LinkPaymentMethod(
        details = linkPaymentDetails.paymentDetails,
        collectedCvc = collectedCvc,
        billingPhone = billingPhone
    )
}
