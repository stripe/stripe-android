package com.stripe.android.networking

import android.content.Context
import com.stripe.android.R
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import java.util.Locale
import com.stripe.android.uicore.R as UiCoreR

internal fun StripeError.withLocalizedMessage(context: Context): StripeError {
    val newMessage = if (shouldFallBackToLocalizedError) {
        context.mapErrorCodeToLocalizedMessage(code)
    } else {
        message ?: context.mapErrorCodeToLocalizedMessage(code)
    }

    return copy(message = newMessage)
}

internal fun PaymentIntent.Error.withLocalizedMessage(context: Context): PaymentIntent.Error {
    val newMessage = if (shouldFallBackToLocalizedError) {
        context.mapErrorCodeToLocalizedMessage(code)
    } else {
        message ?: context.mapErrorCodeToLocalizedMessage(code)
    }

    return copy(message = newMessage)
}

internal fun SetupIntent.Error.withLocalizedMessage(context: Context): SetupIntent.Error {
    val newMessage = if (shouldFallBackToLocalizedError) {
        context.mapErrorCodeToLocalizedMessage(code)
    } else {
        message ?: context.mapErrorCodeToLocalizedMessage(code)
    }

    return copy(message = newMessage)
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

/**
 * For some language tags, our backend is unable to provide translated error messages. For these
 * languages, we fall back to local error messages. As of right now, the only languages for which we
 * are aware of this issue are Spanish languages outside of Spain, such as in Argentina or Chile.
 */
private val shouldFallBackToLocalizedError: Boolean
    get() {
        val locale = Locale.getDefault()
        return locale.language.lowercase() == "es" && locale.country.lowercase() != "es"
    }
