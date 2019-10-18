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
    val type: String? = null,

    val message: String? = null,
    /**
     * [Stripe API Error Codes](https://stripe.com/docs/error-codes)
     * (e.g. "payment_method_unactivated")
     */
    val code: String? = null,

    val param: String? = null,
    /**
     * [Stripe API Decline Codes](https://stripe.com/docs/declines/codes)
     */
    val declineCode: String? = null,

    val charge: String? = null
) : Serializable
