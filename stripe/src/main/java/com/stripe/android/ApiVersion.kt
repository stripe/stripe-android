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
internal data class ApiVersion internal constructor(
    internal val version: String,
    internal var betas: Set<StripeApiBeta>? = null
) {
    constructor(
        betas: Set<StripeApiBeta>
    ) : this(API_VERSION_CODE, betas)

    val code: String
        get() =
            betas?.let {
                listOf(this.version)
                    .plus(
                        betas!!.map { it.code }
                    )
                    .joinToString(";")
            } ?: version

    override fun toString(): String {
        return betas?.let {
            listOf(this.version)
                .plus(
                    betas!!.map { it.code }
                )
                .joinToString(";")
        } ?: version
    }

    internal companion object {
        const val API_VERSION_CODE: String = "2020-03-02"

        private val INSTANCE = ApiVersion(API_VERSION_CODE)

        @JvmSynthetic
        internal fun get(): ApiVersion {
            return INSTANCE
        }
    }
}
