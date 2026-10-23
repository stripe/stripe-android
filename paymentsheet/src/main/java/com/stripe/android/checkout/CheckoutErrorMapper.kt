package com.stripe.android.checkout

import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import javax.inject.Inject

internal class CheckoutErrorMapper @Inject constructor() {
    fun displayMessage(error: Throwable): String? {
        (error as? LocalStripeException)?.displayMessage?.let { return it }

        // Malformed responses contain a parser diagnostic, so omit that message.
        return (error as? StripeException)?.stripeError?.message
            ?.takeIf { it != StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE }
    }
}
