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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentMethodSelection {
    val type: PaymentMethodType

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Card : PaymentMethodSelection {
        override val type = PaymentMethodType.Card
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class BankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.BankAccount
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CardAndBankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.CardAndBankAccount
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class GooglePay(
        internal val currencyCode: String,
        internal val amount: Long,
        internal val transactionId: String? = null,
        internal val label: String? = null
    ) : PaymentMethodSelection {
        override val type = PaymentMethodType.GooglePay
    }
}
