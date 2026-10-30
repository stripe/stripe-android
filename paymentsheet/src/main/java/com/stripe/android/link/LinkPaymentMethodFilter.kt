package com.stripe.android.link

import com.stripe.android.model.ConsumerPaymentDetails

internal enum class LinkPaymentMethodFilter {
    Card,
    BankAccount,
    Generic;

    operator fun invoke(details: ConsumerPaymentDetails.PaymentDetails): Boolean {
        return when (details) {
            is ConsumerPaymentDetails.BankAccount -> this == BankAccount
            is ConsumerPaymentDetails.Card -> this == Card
            is ConsumerPaymentDetails.Generic -> this == Generic
            else -> false
        }
    }

    /**
     * Whether this filter allows payment details of the given type, e.g. a type from
     * [com.stripe.android.model.StripeIntent.linkFundingSources].
     */
    fun allows(paymentDetailsType: String): Boolean {
        return when (paymentDetailsType.lowercase()) {
            ConsumerPaymentDetails.Card.TYPE -> this == Card
            ConsumerPaymentDetails.BankAccount.TYPE -> this == BankAccount
            else -> this == Generic
        }
    }
}
