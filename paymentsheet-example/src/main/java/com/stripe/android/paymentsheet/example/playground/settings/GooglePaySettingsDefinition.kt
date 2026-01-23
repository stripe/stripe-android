package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object GooglePaySettingsDefinition :
    PlaygroundSettingDefinition<GooglePayMode>,
    PlaygroundSettingDefinition.Saveable<GooglePayMode> by EnumSaveable(
        key = "googlePayMode",
        values = GooglePayMode.entries.toTypedArray(),
        defaultValue = GooglePayMode.Test,
    ),
    PlaygroundSettingDefinition.Displayable<GooglePayMode> {
    override val displayName: String = "Google Pay"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ): Boolean {
        return !configurationData.integrationType.isCustomerFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<GooglePayMode>> {
        return GooglePayMode.entries.map {
            PlaygroundSettingDefinition.Displayable.Option(name = it.name, value = it)
        }
    }

    override fun configure(
        value: GooglePayMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.googlePay(
            googlePay = createGooglePayConfiguration(
                configOption = value,
                merchant = playgroundState.merchantCode,
                currency = playgroundState.currencyCode,
            )
        )
    }

    override fun configure(
        value: GooglePayMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.googlePay(
            googlePay = createGooglePayConfiguration(
                configOption = value,
                merchant = playgroundState.merchantCode,
                currency = playgroundState.currencyCode,
            )
        )
    }

    override fun configure(
        value: GooglePayMode,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData,
    ) {
        configurationBuilder.googlePay(
            googlePay = createGooglePayConfiguration(
                configOption = value,
                merchant = playgroundState.merchantCode,
                currency = playgroundState.currencyCode,
            )
        )
    }

    private fun createGooglePayConfiguration(
        configOption: GooglePayMode,
        merchant: Merchant,
        currency: Currency,
    ): PaymentSheet.GooglePayConfiguration? {
        if (configOption == GooglePayMode.Off) {
            return null
        }

        val environment = if (configOption == GooglePayMode.Test) {
            PaymentSheet.GooglePayConfiguration.Environment.Test
        } else {
            PaymentSheet.GooglePayConfiguration.Environment.Production
        }

        return PaymentSheet.GooglePayConfiguration(
            environment = environment,
            countryCode = merchant.countryCode,
            currencyCode = currency.value,
        )
    }
}

enum class GooglePayMode(
    override val value: String
) : ValueEnum {
    Off("off"),
    Test("test"),
    Production("production")
}
