package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.ApiConfigurationPreview
import com.stripe.android.paymentsheet.PaymentSheet

internal sealed class ApiConfigurationTestType(
    val paymentConfigurationPublishableKey: String?,
    val apiConfiguration: ApiConfiguration?,
) {
    fun initializePaymentConfiguration(context: Context) {
        PaymentConfiguration.clearInstance()
        context.getSharedPreferences(
            PaymentConfiguration::class.java.canonicalName,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        paymentConfigurationPublishableKey?.let { publishableKey ->
            PaymentConfiguration.init(context, publishableKey)
        }
    }

    fun withPublishableKey(publishableKey: String): ApiConfigurationTestType {
        return Configured(
            paymentConfigurationPublishableKey = paymentConfigurationPublishableKey?.let {
                if (this is ApiConfigurationOverridesPaymentConfiguration) it else publishableKey
            },
            apiConfiguration = apiConfiguration?.let { configuration ->
                ApiConfiguration(publishableKey).stripeAccountId(configuration.build().stripeAccountId)
            },
        )
    }

    @OptIn(ApiConfigurationPreview::class)
    fun applyTo(configuration: PaymentSheet.Configuration): PaymentSheet.Configuration {
        return apiConfiguration?.let { apiConfiguration ->
            configuration.newBuilder().apiConfiguration(apiConfiguration).build()
        } ?: configuration
    }

    data object PaymentConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = TestApiKeys.PUBLISHABLE,
        apiConfiguration = null,
    )

    data object ApiConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = null,
        apiConfiguration = ApiConfiguration(TestApiKeys.PUBLISHABLE),
    )

    data object ApiConfigurationOverridesPaymentConfiguration : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = "pk_test_should_not_be_used",
        apiConfiguration = ApiConfiguration(TestApiKeys.PUBLISHABLE),
    )

    private class Configured(
        paymentConfigurationPublishableKey: String?,
        apiConfiguration: ApiConfiguration?,
    ) : ApiConfigurationTestType(paymentConfigurationPublishableKey, apiConfiguration)
}

internal object ApiConfigurationTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ApiConfigurationTestType> = listOf(
        ApiConfigurationTestType.PaymentConfigurationOnly,
        ApiConfigurationTestType.ApiConfigurationOnly,
    )
}
