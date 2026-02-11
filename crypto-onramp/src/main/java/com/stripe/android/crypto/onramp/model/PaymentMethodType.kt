package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * The type of payment method to present for selection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentMethodType(internal val value: String) {
    Card("card"),
    BankAccount("bank_account"),
    CardAndBankAccount("card_and_bank_account"),
    GooglePay("google_pay")
}

sealed interface PaymentMethodSelection {
    val type: PaymentMethodType

    class Card : PaymentMethodSelection {
        override val type = PaymentMethodType.Card
    }

    class BankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.BankAccount
    }

    class CardAndBankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.CardAndBankAccount
    }

    class GooglePay(
        val currencyCode: String,
        val amount: Long,
        val transactionId: String? = null,
        val label: String? = null
    ) : PaymentMethodSelection {
        override val type = PaymentMethodType.GooglePay
    }
}
