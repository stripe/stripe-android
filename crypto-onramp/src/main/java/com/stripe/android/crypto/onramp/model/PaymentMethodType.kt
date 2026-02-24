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

/**
 * Represents the available payment method selections supported by the payment flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentMethodSelection {

    /**
     * The [PaymentMethodType] associated with this selection.
     */
    val type: PaymentMethodType

    /**
     * Represents a card-only payment method selection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Card : PaymentMethodSelection {
        override val type = PaymentMethodType.Card
    }

    /**
     * Represents a bank account payment method selection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class BankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.BankAccount
    }

    /**
     * Represents a combined payment method selection allowing both
     * card and bank account options within the same flow.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CardAndBankAccount : PaymentMethodSelection {
        override val type = PaymentMethodType.CardAndBankAccount
    }

    /**
     * Represents a Google Pay payment method selection.
     *
     * Parameters:
     * @param currencyCode The ISO 4217 currency code used for the transaction
     * (e.g., "USD").
     * @param amount The transaction amount
     * @param transactionId Optional identifier associated with the Google Pay
     * transaction.
     * @param label Optional label describing the transaction shown to the user
     * in the Google Pay sheet when supported.
     */
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
