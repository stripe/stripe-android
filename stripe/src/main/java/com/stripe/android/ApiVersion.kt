package com.stripe.android

/**
 * A class that represents a Stripe API version.
 *
 * See [https://stripe.com/docs/api/versioning](https://stripe.com/docs/api/versioning)
 * for documentation on API versioning.
 *
 * See [https://stripe.com/docs/upgrades](https://stripe.com/docs/upgrades) for latest
 * API changes.
 */
internal data class ApiVersion constructor(val code: String) {

    override fun toString(): String {
        return code
    }

    companion object {
        private const val API_VERSION_CODE = "2019-05-16"

        private val INSTANCE = ApiVersion(API_VERSION_CODE)

        @JvmStatic
        fun get(): ApiVersion {
            return INSTANCE
        }
    }
}
