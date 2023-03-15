package com.stripe.android.networking

import android.content.Context
import com.stripe.android.R
import com.stripe.android.core.StripeError

@Suppress("ComplexMethod")
internal fun StripeError.withLocalizedMessage(context: Context): StripeError {
    val newMessage = context.mapErrorCodeToLocalizedMessage(code) ?: message
    return copy(message = newMessage)
}

internal fun Context.mapErrorCodeToLocalizedMessage(code: String?): String? {
    val messageResourceId = when (code) {
        "incorrect_number" -> R.string.invalid_card_number
        "invalid_number" -> R.string.invalid_card_number
        "invalid_expiry_month" -> R.string.invalid_expiry_month
        "invalid_expiry_year" -> R.string.invalid_expiry_year
        "invalid_cvc" -> R.string.invalid_cvc
        "expired_card" -> R.string.expired_card
        "incorrect_cvc" -> R.string.invalid_cvc
        "card_declined" -> R.string.card_declined
        "processing_error" -> R.string.processing_error
        "invalid_owner_name" -> R.string.invalid_owner_name
        "invalid_bank_account_iban" -> R.string.invalid_bank_account_iban
        "generic_decline" -> R.string.generic_decline
        else -> null
    }
    return messageResourceId?.let { getString(it) }
}
