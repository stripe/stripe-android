package com.stripe.android

import com.stripe.android.model.StripeModel
import java.io.Serializable
import kotlinx.android.parcel.Parcelize

/**
 * [Errors](https://stripe.com/docs/api/errors)
 *
 * Stripe uses conventional HTTP response codes to indicate the success or failure of an API
 * request. In general:
 * - Codes in the `2xx` range indicate success.
 * - Codes in the `4xx` range indicate an error that failed given the information provided
 *   (e.g. a required parameter was omitted, a charge failed, etc.).
 * - Codes in the `5xx` range indicate an error with Stripe's servers (these are rare).
 *
 * Some `4xx` errors that could be handled programmatically (e.g., a card is
 * [declined](https://stripe.com/docs/declines)) include an
 * [error code](https://stripe.com/docs/error-codes) that briefly explains the error reported.
 */
@Parcelize
data class StripeError internal constructor(
    /**
     * The type of error returned. One of `api_connection_error`, `api_error`,
     * `authentication_error`, `card_error`, `idempotency_error`, `invalid_request_error`,
     * or `rate_limit_error`
     *
     * [type](https://stripe.com/docs/api/errors#errors-type)
     */
    val type: String? = null,

    /**
     * A human-readable message providing more details about the error. For card errors,
     * these messages can be shown to your users.
     *
     * [message](https://stripe.com/docs/api/errors#errors-message)
     */
    val message: String? = null,

    /**
     * For some errors that could be handled programmatically, a short string indicating the
     * [error code](https://stripe.com/docs/error-codes) reported.
     *
     * [code](https://stripe.com/docs/api/errors#errors-code)
     */
    val code: String? = null,

    /**
     * If the error is parameter-specific, the parameter related to the error. For example, you can
     * use this to display a message near the correct form field.
     *
     * [param](https://stripe.com/docs/api/errors#errors-param)
     */
    val param: String? = null,

    /**
     * For card errors resulting from a card issuer decline, a short string indicating the
     * [card issuer’s reason for the decline](https://stripe.com/docs/declines/codes)
     * if they provide one.
     *
     * [decline_code](https://stripe.com/docs/api/errors#errors-decline_code)
     */
    val declineCode: String? = null,

    /**
     * For card errors, the ID of the failed charge.
     *
     * [charge](https://stripe.com/docs/api/errors#errors-charge)
     */
    val charge: String? = null,

    /**
     * A URL to more information about the
     * [error code](https://stripe.com/docs/error-codes) reported.
     *
     * [doc_url](https://stripe.com/docs/api/errors#errors-doc_url)
     */
    val docUrl: String? = null
) : StripeModel, Serializable
