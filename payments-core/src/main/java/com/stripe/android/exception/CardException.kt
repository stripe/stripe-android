package com.stripe.android.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "cardError"
}
