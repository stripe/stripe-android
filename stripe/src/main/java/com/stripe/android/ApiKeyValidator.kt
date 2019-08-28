package com.stripe.android

internal class ApiKeyValidator {

    fun requireValid(apiKey: String?): String {
        if (apiKey == null || apiKey.trim { it <= ' ' }.isEmpty()) {
            throw IllegalArgumentException("Invalid Publishable Key: " +
                "You must use a valid Stripe API key to make a Stripe API request. " +
                "For more info, see https://stripe.com/docs/keys")
        }

        if (apiKey.startsWith("sk_")) {
            throw IllegalArgumentException("Invalid Publishable Key: " +
                "You are using a secret key instead of a publishable one. " +
                "For more info, see https://stripe.com/docs/keys")
        }

        return apiKey
    }

    companion object {
        private val DEFAULT = ApiKeyValidator()

        @JvmStatic
        fun get(): ApiKeyValidator {
            return DEFAULT
        }
    }
}
