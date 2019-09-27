package com.stripe.android

import java.io.Serializable

/**
 * A model for error objects sent from the Stripe API.
 *
 * [Stripe API Errors](https://stripe.com/docs/api/errors)
 */
data class StripeError internal constructor(
    /**
     * [Stripe API Errors](https://stripe.com/docs/api/errors)
     * (e.g. "invalid_request_error")
     */
    val type: String?,

    val message: String?,
    /**
     * [Stripe API Error Codes](https://stripe.com/docs/error-codes)
     * (e.g. "payment_method_unactivated")
     */
    val code: String?,

    val param: String?,
    /**
     * [Stripe API Decline Codes](https://stripe.com/docs/declines/codes)
     */
    val declineCode: String?,

    val charge: String?
) : Serializable
