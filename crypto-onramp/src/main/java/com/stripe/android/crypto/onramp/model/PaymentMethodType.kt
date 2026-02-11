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
