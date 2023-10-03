package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Stable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.IntegrationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings

@Stable
internal data class PlaygroundState(
    private val snapshot: PlaygroundSettings.Snapshot,
    val amount: Long,
    val paymentMethodTypes: List<String>,
    val customerConfig: PaymentSheet.CustomerConfiguration?,
    val clientSecret: String,
) {
    val initializationType = snapshot[InitializationTypeSettingsDefinition]
    val currencyCode = snapshot[CurrencySettingsDefinition]
    val countryCode = snapshot[CountrySettingsDefinition]
    val checkoutMode = snapshot[CheckoutModeSettingsDefinition]
    val integrationType = snapshot[IntegrationTypeSettingsDefinition]

    fun paymentSheetConfiguration(): PaymentSheet.Configuration {
        return snapshot.paymentSheetConfiguration(this)
    }

    companion object {
        fun CheckoutResponse.asPlaygroundState(
            snapshot: PlaygroundSettings.Snapshot,
        ): PlaygroundState {
            val paymentMethodTypes = if (snapshot[AutomaticPaymentMethodsSettingsDefinition]) {
                emptyList()
            } else {
                paymentMethodTypes
                    .orEmpty()
                    .split(",")
            }
            return PlaygroundState(
                snapshot = snapshot,
                amount = amount,
                paymentMethodTypes = paymentMethodTypes,
                customerConfig = makeCustomerConfig(),
                clientSecret = intentClientSecret,
            )
        }
    }
}
