package com.stripe.android.link.model

import com.stripe.android.link.R
import com.stripe.android.model.ConsumerPaymentDetails

internal val ConsumerPaymentDetails.BankAccount.icon
    get() = R.drawable.stripe_link_bank

internal val ConsumerPaymentDetails.PaymentDetails.removeLabel
    get() = when (this) {
        is ConsumerPaymentDetails.Passthrough, is ConsumerPaymentDetails.Card -> R.string.stripe_wallet_remove_card
        is ConsumerPaymentDetails.BankAccount -> R.string.stripe_wallet_remove_linked_account
    }

internal val ConsumerPaymentDetails.PaymentDetails.removeConfirmation
    get() = when (this) {
        is ConsumerPaymentDetails.Passthrough, is ConsumerPaymentDetails.Card -> R.string.stripe_wallet_remove_card_confirmation
        is ConsumerPaymentDetails.BankAccount -> R.string.stripe_wallet_remove_account_confirmation
    }
