package com.stripe.android.networking

import android.content.Context
import com.stripe.android.R
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.uicore.R as UiCoreR

internal fun StripeError.withLocalizedMessage(context: Context): StripeError {
    return copy(
        message = getErrorMessage(
            originalMessage = message,
            code = code,
            declineCode = declineCode,
            context = context,
            isCardError = this.type == "card_error",
        )
    )
}

internal fun PaymentIntent.Error.withLocalizedMessage(context: Context): PaymentIntent.Error {
    return copy(
        message = getErrorMessage(
            originalMessage = message,
            code = code,
            declineCode = declineCode,
            context = context,
            isCardError = this.type == PaymentIntent.Error.Type.CardError,
        )
    )
}

internal fun SetupIntent.Error.withLocalizedMessage(context: Context): SetupIntent.Error {
    return copy(
        message = getErrorMessage(
            originalMessage = message,
            code = code,
            declineCode = declineCode,
            context = context,
            isCardError = this.type == SetupIntent.Error.Type.CardError
        )
    )
}

internal fun Context.mapErrorCodeToLocalizedMessage(code: String?): String? {
    val messageResourceId = when (code) {
        "incorrect_number" -> R.string.stripe_invalid_card_number
        "invalid_number" -> R.string.stripe_invalid_card_number
        "invalid_expiry_month" -> UiCoreR.string.stripe_invalid_expiry_month
        "invalid_expiry_year" -> UiCoreR.string.stripe_invalid_expiry_year
        "invalid_cvc" -> R.string.stripe_invalid_cvc
        "expired_card" -> R.string.stripe_expired_card
        "incorrect_cvc" -> R.string.stripe_invalid_cvc
        "card_declined" -> R.string.stripe_card_declined
        "processing_error" -> R.string.stripe_processing_error
        "invalid_owner_name" -> R.string.stripe_invalid_owner_name
        "invalid_bank_account_iban" -> R.string.stripe_invalid_bank_account_iban
        "generic_decline" -> R.string.stripe_generic_decline
        else -> null
    }
    return messageResourceId?.let { getString(it) }
}

private fun getErrorMessage(
    originalMessage: String?,
    code: String?,
    declineCode: String?,
    context: Context,
    isCardError: Boolean,
): String {
    // https://docs.stripe.com/api/errors#errors-message
    // A human-readable message providing more details about the error.
    // For card errors, these messages can be shown to your users.
    return originalMessage.takeIf { isCardError }
        // https://docs.stripe.com/error-codes
        ?: context.mapErrorCodeToLocalizedMessage(code)
        // https://docs.stripe.com/declines/codes
        ?: context.mapErrorCodeToLocalizedMessage(declineCode)
        ?: "There was an unexpected error -- try again in a few seconds"
}
