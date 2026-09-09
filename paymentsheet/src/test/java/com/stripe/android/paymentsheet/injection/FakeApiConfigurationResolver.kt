package com.stripe.android.paymentsheet.injection

import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.ApiConfiguration

internal class FakeApiConfigurationResolver(
    private val resolvedApiConfiguration: ApiConfiguration.State = ApiConfiguration.State(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        stripeAccountId = ApiKeyFixtures.FAKE_ACCOUNT_ID
    ),
) : ApiConfigurationResolver {
    override fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        return apiConfiguration ?: resolvedApiConfiguration
    }
}
