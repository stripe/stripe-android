package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.TestApiKeys

internal sealed class ApiConfigurationTestType(
    val paymentConfigurationPublishableKey: String,
    val paymentConfigurationStripeAccount: String
) {
    fun initializePaymentConfiguration(context: Context) {
        PaymentConfiguration.clearInstance()
        context.getSharedPreferences(
            PaymentConfiguration::class.java.canonicalName,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        PaymentConfiguration.init(context, paymentConfigurationPublishableKey, paymentConfigurationStripeAccount)
    }

    fun withPublishableKey(publishableKey: String): ApiConfigurationTestType {
        return Configured(publishableKey, paymentConfigurationStripeAccount)
    }

    data object PaymentConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = TestApiKeys.PUBLISHABLE,
        paymentConfigurationStripeAccount = TestApiKeys.ACCOUNT
    )

    private class Configured(
        paymentConfigurationPublishableKey: String,
        paymentConfigurationStripeAccount: String
    ) : ApiConfigurationTestType(paymentConfigurationPublishableKey, paymentConfigurationStripeAccount)
}
