package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG

internal class FakeApiConfigurationResolver(
    private val resolvedApiConfiguration: ApiConfiguration.State = DEFAULT_API_CONFIG,
) : ApiConfigurationResolver {
    override fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        return apiConfiguration ?: resolvedApiConfiguration
    }
}
