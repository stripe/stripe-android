package com.stripe.android.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ApiKeyValidator {

    fun requireValid(apiKey: String?): String {
        require(!apiKey.isNullOrBlank()) {
            "Invalid Publishable Key: " +
                "You must use a valid Stripe API key to make a Stripe API request. " +
                "For more info, see https://stripe.com/docs/keys"
        }

        require(!apiKey.startsWith("sk_")) {
            "Invalid Publishable Key: " +
                "You are using a secret key instead of a publishable one. " +
                "For more info, see https://stripe.com/docs/keys"
        }

        return apiKey
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private val DEFAULT = ApiKeyValidator()

        @JvmStatic
        fun get(): ApiKeyValidator {
            return DEFAULT
        }
    }
}
