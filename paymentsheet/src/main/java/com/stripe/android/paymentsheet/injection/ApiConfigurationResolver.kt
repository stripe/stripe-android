package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import javax.inject.Inject

internal class ApiConfigurationResolver @Inject constructor(
    private val context: Context,
) {
    fun resolve(apiConfiguration: ApiConfiguration.State?): ApiConfiguration.State {
        if (apiConfiguration != null) return apiConfiguration
        val config = PaymentConfiguration.getInstance(context)
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}
