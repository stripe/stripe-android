package com.stripe.android.view.i18n

import com.stripe.android.core.StripeError

interface ErrorMessageTranslator {
    /**
     * See [Stripe API Errors](https://stripe.com/docs/api/errors) for a list of error
     * codes and associated messages.
     *
     * @param httpCode The HTTP code associated with the error response.
     * @param errorMessage A human-readable message providing more details about the error.
     * For card errors, these messages can be shown to your users.
     * @param stripeError The [StripeError] that represents detailed information about the
     * error. Specifically, [StripeError.code] is useful for understanding
     * the underlying error (e.g. "payment_method_unactivated").
     *
     * @return a non-null error message
     */
    fun translate(httpCode: Int, errorMessage: String?, stripeError: StripeError?): String
}
