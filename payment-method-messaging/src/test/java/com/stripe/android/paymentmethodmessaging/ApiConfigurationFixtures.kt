package com.stripe.android.paymentmethodmessaging

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest

internal object ApiConfigurationFixtures {
    val DEFAULT_API_CONFIG = ApiConfiguration.State(
        publishableKey = "pk_test_123",
        stripeAccountId = "acct_123",
    )

    val DEFAULT_REQUEST_OPTIONS = ApiRequest.Options(
        apiKey = DEFAULT_API_CONFIG.publishableKey,
        stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
    )
}
