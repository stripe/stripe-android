package com.stripe.android.paymentsheet.state

import android.content.Context
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration

internal object ApiConfigurationResolver {
    fun resolve(
        apiConfiguration: ApiConfiguration.State?,
        context: Context
    ): ApiConfiguration.State {
        return apiConfiguration ?: PaymentConfiguration.getInstance(context).let {
            ApiConfiguration.State(
                publishableKey = it.publishableKey,
                stripeAccountId = it.stripeAccountId
            )
        }
    }
}
