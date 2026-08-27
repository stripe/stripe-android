package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import javax.inject.Inject

internal class ApiConfigurationResolver(
    private val paymentConfigurationProvider: () -> PaymentConfiguration,
) {
    @Inject
    constructor(context: Context) : this(
        paymentConfigurationProvider = { PaymentConfiguration.getInstance(context) }
    )

    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = paymentConfigurationProvider()
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
