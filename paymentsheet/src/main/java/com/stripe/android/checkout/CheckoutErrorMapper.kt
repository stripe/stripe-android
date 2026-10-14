package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import javax.inject.Inject

internal class CheckoutErrorMapper @Inject constructor(
    private val context: Context,
) {
    fun displayMessage(error: Throwable): String {
        val localMessage = (error as? LocalStripeException)?.displayMessage
        // The parser fallback is diagnostic text rather than a message supplied by the server.
        val serverMessage = (error as? StripeException)?.stripeError
            ?.message?.takeUnless { it == StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE }
        return localMessage ?: serverMessage ?: error.stripeErrorMessage().resolve(context)
    }

    fun populateDisplayMessage(error: Throwable) {
        (error as? StripeException)?.setCheckoutDisplayMessage(displayMessage(error))
    }
}
