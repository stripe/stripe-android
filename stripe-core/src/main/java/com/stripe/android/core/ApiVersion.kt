package com.stripe.android.core

import androidx.annotation.RestrictTo

/**
 * A class that represents a Stripe API version.
 *
 * See [https://stripe.com/docs/api/versioning](https://stripe.com/docs/api/versioning)
 * for documentation on API versioning.
 *
 * See [https://stripe.com/docs/upgrades](https://stripe.com/docs/upgrades) for latest
 * API changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ApiVersion internal constructor(
    internal val version: String,
    internal val betaCodes: Set<String> = emptySet()
) {
    constructor(
        betas: Set<String>
    ) : this(API_VERSION_CODE, betas)

    val code: String
        get() =
            listOf(this.version)
                .plus(
                    betaCodes.map { it }
                )
                .joinToString(";")

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val API_VERSION_CODE: String = "2020-03-02"

        private val INSTANCE = ApiVersion(API_VERSION_CODE)

        @JvmSynthetic
        fun get(): ApiVersion {
            return INSTANCE
        }
    }
}
