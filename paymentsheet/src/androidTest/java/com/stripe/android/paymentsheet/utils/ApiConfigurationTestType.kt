package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.ApiConfigurationPreview
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.paymentsheet.PaymentSheet

internal sealed class ApiConfigurationTestType(
    val paymentConfigurationPublishableKey: String?,
    val paymentConfigurationStripeAccount: String?,
    val apiConfiguration: ApiConfiguration?
) {
    fun initializePaymentConfiguration(context: Context) {
        PaymentConfiguration.clearInstance()
        context.getSharedPreferences(
            PaymentConfiguration::class.java.canonicalName,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        paymentConfigurationPublishableKey?.let {
            PaymentConfiguration.init(context, it, paymentConfigurationStripeAccount)
        }
    }

    fun withPublishableKey(publishableKey: String): ApiConfigurationTestType {
        return Configured(
            paymentConfigurationPublishableKey = paymentConfigurationPublishableKey?.let { publishableKey },
            paymentConfigurationStripeAccount = paymentConfigurationStripeAccount,
            apiConfiguration = apiConfiguration?.let {
                ApiConfiguration(publishableKey).stripeAccountId(it.build().stripeAccountId)
            }
        )
    }

    @OptIn(ApiConfigurationPreview::class)
    fun applyTo(configuration: PaymentSheet.Configuration): PaymentSheet.Configuration {
        return apiConfiguration?.let {
            configuration.newBuilder().apiConfiguration(it).build()
        } ?: configuration
    }

    data object PaymentConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = TestApiKeys.PUBLISHABLE,
        paymentConfigurationStripeAccount = TestApiKeys.ACCOUNT,
        apiConfiguration = null
    )

    data object ApiConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = null,
        paymentConfigurationStripeAccount = null,
        apiConfiguration = ApiConfiguration(TestApiKeys.PUBLISHABLE).stripeAccountId(TestApiKeys.ACCOUNT)
    )

    private class Configured(
        paymentConfigurationPublishableKey: String?,
        paymentConfigurationStripeAccount: String?,
        apiConfiguration: ApiConfiguration?
    ) : ApiConfigurationTestType(paymentConfigurationPublishableKey, paymentConfigurationStripeAccount, apiConfiguration)
}

internal object ApiConfigurationTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ApiConfigurationTestType> = listOf(
        ApiConfigurationTestType.PaymentConfigurationOnly,
        ApiConfigurationTestType.ApiConfigurationOnly,
    )
}
