package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.paymentsheet.R
import javax.inject.Inject

internal class CheckoutDisplayMessageResolver @Inject constructor(
    context: Context,
) {
    private val genericErrorMessage = context.getString(R.string.stripe_something_went_wrong)

    fun resolve(error: Throwable): String {
        (error as? LocalStripeException)?.displayMessage?.let { return it }

        // Malformed responses contain a parser diagnostic, so omit that message.
        return (error as? StripeException)?.stripeError?.message
            ?.takeIf { it != StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE }
            ?: genericErrorMessage
    }
}
