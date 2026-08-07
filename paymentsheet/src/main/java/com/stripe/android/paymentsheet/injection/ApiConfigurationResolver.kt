package com.stripe.android.paymentsheet.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject
import javax.inject.Provider

internal class ApiConfigurationResolver @Inject constructor(
    private val paymentConfiguration: Provider<PaymentConfiguration>,
) {
    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = paymentConfiguration.get()
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
