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
        (error as? LocalStripeException)?.displayMessage?.let { return it }

        (error as? StripeException)?.stripeError?.message
            ?.takeIf { it != StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE }
            ?.let { return it }

        return error.stripeErrorMessage().resolve(context)
    }
}
