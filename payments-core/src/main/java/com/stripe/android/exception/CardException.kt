package com.stripe.android.exception

import com.stripe.android.StripeError
import java.net.HttpURLConnection

/**
 * An [Exception] indicating that there is a problem with a Card used for a request.
 * Card errors are the most common type of error you should expect to handle.
 * They result when the user enters a card that can't be charged for some reason.
 */
class CardException(
    stripeError: StripeError,
    requestId: String? = null
) : StripeException(
    stripeError,
    requestId,
    HttpURLConnection.HTTP_PAYMENT_REQUIRED
) {
    val code: String? = stripeError.code
    val param: String? = stripeError.param
    val declineCode: String? = stripeError.declineCode
    val charge: String? = stripeError.charge
}
