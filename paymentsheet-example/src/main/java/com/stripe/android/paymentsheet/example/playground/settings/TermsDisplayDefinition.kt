package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object TermsDisplaySettingsDefinition :
    PlaygroundSettingDefinition<TermsDisplay>,
    PlaygroundSettingDefinition.Saveable<TermsDisplay> by EnumSaveable(
        key = "termsDisplay",
        values = TermsDisplay.entries.toTypedArray(),
        defaultValue = TermsDisplay.AUTOMATIC,
    ),
    PlaygroundSettingDefinition.Displayable<TermsDisplay> {
    override val displayName: String = "Terms Display"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<TermsDisplay>> {
        return TermsDisplay.entries.map { termsDisplay ->
            option(name = termsDisplay.value, value = termsDisplay)
        }
    }

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: TermsDisplay,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.termsDisplay(value.termsDisplay)
    }

    override fun configure(
        value: TermsDisplay,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.termsDisplay(value.termsDisplay)
    }
}

enum class TermsDisplay(
    override val value: String,
    val termsDisplay: Map<PaymentMethod.Type, PaymentSheet.TermsDisplay>
) : ValueEnum {
    AUTOMATIC("Auto", emptyMap()),
    NEVER_CARDS("Never Cards", mapOf(PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER)),
    NEVER_US_BANK("Never US Bank", mapOf(PaymentMethod.Type.USBankAccount to PaymentSheet.TermsDisplay.NEVER)),
    NEVER_CASH_APP("Never CashApp", mapOf(PaymentMethod.Type.CashAppPay to PaymentSheet.TermsDisplay.NEVER)),
    NEVER_CASH_AUTO_OTHERS(
        "Never CashApp Auto Others",
        mapOf(
            PaymentMethod.Type.CashAppPay to PaymentSheet.TermsDisplay.NEVER,
            PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.AUTOMATIC,
            PaymentMethod.Type.Klarna to PaymentSheet.TermsDisplay.AUTOMATIC,
            PaymentMethod.Type.AmazonPay to PaymentSheet.TermsDisplay.AUTOMATIC,
        )
    ),
    NEVER_SEPA_FAMILY(
        "Never Sepa Family",
        mapOf(
            PaymentMethod.Type.SepaDebit to PaymentSheet.TermsDisplay.NEVER,
            PaymentMethod.Type.Ideal to PaymentSheet.TermsDisplay.NEVER,
            PaymentMethod.Type.Bancontact to PaymentSheet.TermsDisplay.NEVER,
            PaymentMethod.Type.Sofort to PaymentSheet.TermsDisplay.NEVER,
        )
    ),
}
