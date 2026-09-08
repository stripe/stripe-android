package com.stripe.android.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ApiConfigProviderFromPaymentConfig {
    fun get(context: Context): Provider<ApiConfiguration.State> = Provider {
        val config = PaymentConfiguration.getInstance(context)
        ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId
        )
    }
}
