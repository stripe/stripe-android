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
internal data class ApiVersion internal constructor(internal val code: String) {

    override fun toString(): String {
        return code
    }

    internal companion object {
        private const val API_VERSION_CODE: String = "2020-03-02"

        private val INSTANCE = ApiVersion(API_VERSION_CODE)

        @JvmSynthetic
        internal fun get(): ApiVersion {
            return INSTANCE
        }
    }
}
