package com.stripe.android.core.networking

/**
 * The identifier for a Stripe API request.
 *
 * See https://stripe.com/docs/api/request_ids
 */
internal data class RequestId(
    val value: String
) {
    override fun toString(): String = value

    internal companion object {
        fun fromString(
            value: String?
        ): RequestId? {
            return value.takeUnless { it.isNullOrBlank() }?.let {
                RequestId(it)
            }
        }
    }
}
