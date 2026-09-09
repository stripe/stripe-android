package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import javax.inject.Inject

internal interface ApiConfigurationResolver {
    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State
}

internal class DefaultApiConfigurationResolver @Inject constructor(
    private val context: Context,
) : ApiConfigurationResolver {
    override fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = PaymentConfiguration.getInstance(context)
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
