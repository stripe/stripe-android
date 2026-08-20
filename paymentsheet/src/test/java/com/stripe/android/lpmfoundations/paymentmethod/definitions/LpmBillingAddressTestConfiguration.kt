package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.uicore.elements.IdentifierSpec

internal data class LpmBillingAddressTestConfiguration(
    val paymentMethodType: PaymentMethod.Type,
    val billingDetailsCollectionMode: LpmBillingDetailsCollectionMode,
    val intentScenario: IntentScenario,
    val termsDisplay: PaymentSheet.TermsDisplay,
) {
    fun metadata(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(paymentMethodType),
            billingDetailsCollectionConfiguration = billingDetailsCollectionMode
                .billingDetailsCollectionConfiguration(),
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            termsDisplay = mapOf(paymentMethodType to termsDisplay),
        )
    }

    override fun toString(): String {
        return "${paymentMethodType.code}-${billingDetailsCollectionMode.name}-" +
            "${intentScenario.name}-${termsDisplay.name}"
    }

    internal enum class IntentScenario {
        PaymentIntent,
        PaymentIntentWithSetupFutureUsage,
        SetupIntent,
        ;

        fun stripeIntent(paymentMethodType: PaymentMethod.Type): StripeIntent {
            return when (this) {
                PaymentIntent -> PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                )
                PaymentIntentWithSetupFutureUsage -> PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                    setupFutureUsage = StripeIntent.Usage.OffSession,
                )
                SetupIntent -> SetupIntentFactory.create(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                )
            }
        }
    }
}

internal enum class LpmBillingDetailsCollectionMode {
    Never,
    AutomaticWithoutTax,
    Full,
    ;

    fun billingDetailsCollectionConfiguration(): PaymentSheet.BillingDetailsCollectionConfiguration {
        return when (this) {
            Never -> PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
            AutomaticWithoutTax -> PaymentSheet.BillingDetailsCollectionConfiguration()
            Full -> PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        }
    }
}

internal data class LpmBillingAddressFormValuesToParamsTestCase(
    val name: String,
    val config: LpmBillingAddressTestConfiguration,
    val rawValues: Map<IdentifierSpec, String?>,
    val expectedParams: LpmBillingAddressFormParams,
) {
    override fun toString(): String = name
}

internal data class LpmBillingAddressFormParams(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams?,
    val extraParams: PaymentMethodExtraParams?,
)

internal fun Map<String, Any>.flattenParams(prefix: String = ""): Map<String, Any> {
    return buildMap {
        this@flattenParams.forEach { (key, value) ->
            val flattenedKey = if (prefix.isEmpty()) key else "$prefix.$key"
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val nestedMap = value as Map<String, Any>
                putAll(nestedMap.flattenParams(flattenedKey))
            } else {
                put(flattenedKey, value)
            }
        }
    }
}

internal fun Map<String, Any>.withoutClientAttributionMetadata(): Map<String, Any> {
    return filterKeys { !it.startsWith("client_attribution_metadata.") }
}

internal val lpmBillingAddressFormValuesToParamsTestCases = buildList {
    addAll(boletoTestCases)
    addAll(sepaDebitTestCases)
    addAll(weroTestCases)
    addAll(klarnaTestCases)
    addAll(bacsDebitTestCases)
    addAll(oxxoTestCases)
    addAll(auBecsDebitTestCases)
    addAll(blikTestCases)
    addAll(p24TestCases)
    addAll(epsTestCases)
    addAll(konbiniTestCases)
    addAll(mobilePayTestCases)
    addAll(multibancoTestCases)
    addAll(promptPayTestCases)
    addAll(idealFormParamsTestCases)
}

internal val lpmBillingAddressTestConfigurations =
    lpmBillingAddressFormValuesToParamsTestCases.map { it.config }

internal object LpmBillingAddressFormValuesToParamsTestCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<LpmBillingAddressFormValuesToParamsTestCase> {
        return lpmBillingAddressFormValuesToParamsTestCases
    }
}

internal object LpmBillingAddressTestConfigurationProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<LpmBillingAddressTestConfiguration> {
        return lpmBillingAddressTestConfigurations
    }
}
