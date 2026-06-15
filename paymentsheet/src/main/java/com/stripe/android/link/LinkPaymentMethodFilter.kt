package com.stripe.android.link

import com.stripe.android.model.ConsumerPaymentDetails

internal enum class LinkPaymentMethodFilter {
    Card,
    BankAccount;

    operator fun invoke(details: ConsumerPaymentDetails.PaymentDetails): Boolean {
        return when (details) {
            is ConsumerPaymentDetails.BankAccount -> this == BankAccount
            is ConsumerPaymentDetails.Card -> this == Card
            else -> false
        }
    }
}
