package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object ExternalPaymentMethodSettingsDefinition :
    PlaygroundSettingDefinition<ExternalPaymentMethodType>,
    PlaygroundSettingDefinition.Saveable<ExternalPaymentMethodType> by EnumSaveable(
        key = "externalPaymentMethods",
        values = ExternalPaymentMethodType.entries.toTypedArray(),
        defaultValue = ExternalPaymentMethodType.Off,
    ),
    PlaygroundSettingDefinition.Displayable<ExternalPaymentMethodType> {
    override val displayName: String = "External payment methods"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = ExternalPaymentMethodType.entries.map {
        option(it.displayName, it)
    }

    override fun convertToValue(value: String): ExternalPaymentMethodType {
        val possibleExternalPaymentMethods = value.split(",")

        return ExternalPaymentMethodType.entries.find { it.externalPaymentMethods == possibleExternalPaymentMethods }
            ?: ExternalPaymentMethodType.Off
    }

    override fun convertToString(value: ExternalPaymentMethodType): String = value.value

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: ExternalPaymentMethodType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        when (value) {
            ExternalPaymentMethodType.Off -> Unit
            ExternalPaymentMethodType.Fawry,
            ExternalPaymentMethodType.PayPalAndVenmo,
            ExternalPaymentMethodType.All ->
                configurationBuilder.externalPaymentMethods(value.externalPaymentMethods)
        }
    }

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: ExternalPaymentMethodType,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        when (value) {
            ExternalPaymentMethodType.Off -> Unit
            ExternalPaymentMethodType.Fawry,
            ExternalPaymentMethodType.PayPalAndVenmo,
            ExternalPaymentMethodType.All ->
                configurationBuilder.externalPaymentMethods(value.externalPaymentMethods)
        }
    }
}

enum class ExternalPaymentMethodType(val externalPaymentMethods: List<String>, val displayName: String) : ValueEnum {
    All(externalPaymentMethods = allExternalPaymentMethods, displayName = "All"),
    Fawry(externalPaymentMethods = listOf("external_fawry"), displayName = "Fawry"),
    PayPalAndVenmo(
        externalPaymentMethods = listOf("external_paypal", "external_venmo"),
        displayName = "PayPal and Venmo"
    ),
    Off(externalPaymentMethods = emptyList(), displayName = "Off"),
    ;

    override val value: String
        get() = this.externalPaymentMethods.joinToString(",")
}

private val allExternalPaymentMethods = listOf(
    "external_aplazame",
    "external_atone",
    "external_au_easy_payment",
    "external_au_pay",
    "external_azupay",
    "external_bank_pay",
    "external_benefit",
    "external_billie",
    "external_bitcash",
    "external_bizum",
    "external_catch",
    "external_dapp",
    "external_dbarai",
    "external_divido",
    "external_famipay",
    "external_fawry",
    "external_fonix",
    "external_gcash",
    "external_grabpay_later",
    "external_interac",
    "external_iwocapay",
    "external_kbc",
    "external_knet",
    "external_kriya",
    "external_laybuy",
    "external_line_pay",
    "external_merpay",
    "external_momo",
    "external_mondu",
    "external_net_cash",
    "external_nexi_pay",
    "external_octopus",
    "external_oney",
    "external_paidy",
    "external_pay_easy",
    "external_payconiq",
    "external_paypal",
    "external_paypay",
    "external_paypo",
    "external_paysafecard",
    "external_picpay",
    "external_planpay",
    "external_pledg",
    "external_postepay",
    "external_postfinance",
    "external_rakuten_pay",
    "external_samsung_pay",
    "external_satispay",
    "external_scalapay",
    "external_sequra",
    "external_sezzle",
    "external_shopback_paylater",
    "external_softbank_carrier_payment",
    "external_tabby",
    "external_tng_ewallet",
    "external_toss_pay",
    "external_truelayer",
    "external_twint",
    "external_venmo",
    "external_walley",
    "external_webmoney",
    "external_younited_pay",
)
