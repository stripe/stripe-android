package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Stable
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodConfigurationSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.RequireCvcRecollectionDefinition

@Stable
internal sealed interface PlaygroundState {
    val integrationType: PlaygroundConfigurationData.IntegrationType
    val countryCode: Country
    val endpoint: String

    @Stable
    data class Payment(
        private val snapshot: PlaygroundSettings.Snapshot,
        val amount: Long,
        val paymentMethodTypes: List<String>,
        val customerConfig: PaymentSheet.CustomerConfiguration?,
        val clientSecret: String,
        private val defaultEndpoint: String,
    ) : PlaygroundState {
        override val integrationType = snapshot.configurationData.integrationType
        override val countryCode = snapshot[CountrySettingsDefinition]

        val initializationType = snapshot[InitializationTypeSettingsDefinition]
        val checkoutMode = snapshot[CheckoutModeSettingsDefinition]
        val currencyCode = snapshot[CurrencySettingsDefinition]
        val paymentMethodConfigurationId: String? =
            snapshot[PaymentMethodConfigurationSettingsDefinition].ifEmpty { null }

        val stripeIntentId: String
            get() = clientSecret.substringBefore("_secret_")

        val requireCvcRecollectionForDeferred = snapshot[RequireCvcRecollectionDefinition]

        override val endpoint: String
            get() = snapshot[CustomEndpointDefinition] ?: defaultEndpoint

        fun paymentSheetConfiguration(): PaymentSheet.Configuration {
            return snapshot.paymentSheetConfiguration(this)
        }
    }

    @Stable
    @OptIn(ExperimentalCustomerSheetApi::class)
    data class Customer(
        private val snapshot: PlaygroundSettings.Snapshot,
        override val endpoint: String,
    ) : PlaygroundState {
        override val integrationType = snapshot.configurationData.integrationType
        override val countryCode = snapshot[CountrySettingsDefinition]

        fun customerSheetConfiguration(): CustomerSheet.Configuration {
            return snapshot.customerSheetConfiguration(this)
        }
    }

    fun asPaymentState(): Payment? {
        return this as? Payment
    }

    fun asCustomerState(): Customer? {
        return this as? Customer
    }

    companion object {
        fun CheckoutResponse.asPlaygroundState(
            snapshot: PlaygroundSettings.Snapshot,
            defaultEndpoint: String,
        ): PlaygroundState {
            val paymentMethodTypes = if (snapshot[AutomaticPaymentMethodsSettingsDefinition]) {
                emptyList()
            } else {
                paymentMethodTypes
                    .orEmpty()
                    .split(",")
            }
            return Payment(
                snapshot = snapshot,
                amount = amount,
                paymentMethodTypes = paymentMethodTypes,
                customerConfig = makeCustomerConfig(snapshot.checkoutRequest().customerKeyType),
                clientSecret = intentClientSecret,
                defaultEndpoint = defaultEndpoint
            )
        }
    }
}
