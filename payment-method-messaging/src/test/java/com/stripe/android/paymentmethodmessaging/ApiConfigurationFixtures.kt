package com.stripe.android.paymentmethodmessaging

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.RequestCredentials

internal object ApiConfigurationFixtures {
    val DEFAULT_API_CONFIG = ApiConfiguration.State(
        publishableKey = "pk_test_123",
        stripeAccountId = "acct_123",
        betas = emptySet(),
    )

    val DEFAULT_REQUEST_OPTIONS = RequestCredentials(
        apiKey = DEFAULT_API_CONFIG.publishableKey,
        stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
    )
}
