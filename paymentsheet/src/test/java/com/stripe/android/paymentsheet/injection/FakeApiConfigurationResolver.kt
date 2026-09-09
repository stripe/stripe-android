package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.ApiConfiguration

internal class FakeApiConfigurationResolver(
    private val resolvedApiConfiguration: ApiConfiguration.State,
) : ApiConfigurationResolver {
    override fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        return apiConfiguration ?: resolvedApiConfiguration
    }
}
