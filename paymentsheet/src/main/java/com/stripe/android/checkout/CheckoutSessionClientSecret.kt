package com.stripe.android.checkout

internal data class CheckoutSessionClientSecret(val value: String) {
    val sessionId: String = value.substringBefore(DELIMITER, missingDelimiterValue = "")

    init {
        require(sessionId.isNotEmpty()) {
            "Invalid Checkout Session client secret. " +
                "Pass the client_secret from the Checkout Session created by your server."
        }
    }

    private companion object {
        const val DELIMITER = "_secret_"
    }
}
