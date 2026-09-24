package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.paymentsheet.PaymentSheet

internal sealed class ApiConfigurationTestType(
    val paymentConfigurationPublishableKey: String?,
    val paymentConfigurationStripeAccount: String?,
    val apiConfiguration: ApiConfiguration?,
    private val updatePaymentConfigurationPublishableKey: Boolean,
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
            paymentConfigurationPublishableKey = if (updatePaymentConfigurationPublishableKey) {
                paymentConfigurationPublishableKey?.let { publishableKey }
            } else {
                paymentConfigurationPublishableKey
            },
            paymentConfigurationStripeAccount = paymentConfigurationStripeAccount,
            apiConfiguration = apiConfiguration?.let {
                ApiConfiguration(publishableKey).stripeAccountId(it.build().stripeAccountId)
            },
            updatePaymentConfigurationPublishableKey = updatePaymentConfigurationPublishableKey,
        )
    }

    fun applyTo(configuration: PaymentSheet.Configuration): PaymentSheet.Configuration {
        return apiConfiguration?.let {
            configuration.newBuilder().apiConfiguration(it).build()
        } ?: configuration
    }

    data object PaymentConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = TestApiKeys.PUBLISHABLE,
        paymentConfigurationStripeAccount = TestApiKeys.ACCOUNT,
        apiConfiguration = null,
        updatePaymentConfigurationPublishableKey = true,
    )

    data object ApiConfigurationOnly : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = null,
        paymentConfigurationStripeAccount = null,
        apiConfiguration = ApiConfiguration(TestApiKeys.PUBLISHABLE).stripeAccountId(TestApiKeys.ACCOUNT),
        updatePaymentConfigurationPublishableKey = false,
    )

    data object ApiConfigurationOverridesPaymentConfiguration : ApiConfigurationTestType(
        paymentConfigurationPublishableKey = IGNORED_PAYMENT_CONFIGURATION_PUBLISHABLE_KEY,
        paymentConfigurationStripeAccount = IGNORED_PAYMENT_CONFIGURATION_STRIPE_ACCOUNT,
        apiConfiguration = ApiConfiguration(TestApiKeys.PUBLISHABLE).stripeAccountId(TestApiKeys.ACCOUNT),
        updatePaymentConfigurationPublishableKey = false,
    )

    private class Configured(
        paymentConfigurationPublishableKey: String?,
        paymentConfigurationStripeAccount: String?,
        apiConfiguration: ApiConfiguration?,
        updatePaymentConfigurationPublishableKey: Boolean,
    ) : ApiConfigurationTestType(
        paymentConfigurationPublishableKey,
        paymentConfigurationStripeAccount,
        apiConfiguration,
        updatePaymentConfigurationPublishableKey,
    )

    private companion object {
        const val IGNORED_PAYMENT_CONFIGURATION_PUBLISHABLE_KEY = "pk_test_fake"
        const val IGNORED_PAYMENT_CONFIGURATION_STRIPE_ACCOUNT = "acct_test_fake"
    }
}

internal object ApiConfigurationTestTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ApiConfigurationTestType> = listOf(
        ApiConfigurationTestType.PaymentConfigurationOnly,
        ApiConfigurationTestType.ApiConfigurationOnly,
        ApiConfigurationTestType.ApiConfigurationOverridesPaymentConfiguration,
    )
}
