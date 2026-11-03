package com.stripe.android.paymentmethodmessaging

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.toRequestOptions

internal object ApiConfigurationFixtures {
    val DEFAULT_API_CONFIG = ApiConfiguration.State(
        publishableKey = "pk_test_123",
        stripeAccountId = "acct_123",
    )

    val DEFAULT_REQUEST_OPTIONS = DEFAULT_API_CONFIG.toRequestOptions(idempotencyKey = null)
}
