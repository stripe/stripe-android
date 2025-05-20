package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
internal data class LinkPaymentMethod(
    val details: ConsumerPaymentDetails.PaymentDetails,
    val collectedCvc: String?
) : Parcelable {

    fun readyForConfirmation(): Boolean = when (details) {
        is ConsumerPaymentDetails.BankAccount -> true
        is ConsumerPaymentDetails.Card -> {
            val cvcReady = !details.cvcCheck.requiresRecollection || collectedCvc?.isNotEmpty() == true
            !details.isExpired && cvcReady
        }
        is ConsumerPaymentDetails.Passthrough -> true
    }
}
