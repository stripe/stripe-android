package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.TestApiKeys

internal sealed class ApiConfigurationTestType(
    val paymentConfigurationPublishableKey: String,
) {
    fun initializePaymentConfiguration(context: Context) {
        PaymentConfiguration.clearInstance()
        context.getSharedPreferences(
            PaymentConfiguration::class.java.canonicalName,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        PaymentConfiguration.init(context, paymentConfigurationPublishableKey)
    }

    fun withPublishableKey(publishableKey: String): ApiConfigurationTestType {
        return Configured(publishableKey)
    }

    data object PaymentConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = TestApiKeys.PUBLISHABLE,
    )

    private class Configured(
        paymentConfigurationPublishableKey: String,
    ) : ApiConfigurationTestType(paymentConfigurationPublishableKey)
}

internal object ApiConfigurationTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ApiConfigurationTestType> = listOf(
        ApiConfigurationTestType.PaymentConfigurationOnly,
    )
}
