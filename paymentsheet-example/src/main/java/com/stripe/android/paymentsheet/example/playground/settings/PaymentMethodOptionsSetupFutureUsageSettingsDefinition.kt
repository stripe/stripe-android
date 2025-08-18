package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object PaymentMethodOptionsSetupFutureUsageSettingsDefinition :
    PlaygroundSettingDefinition<PaymentMethodOptionsSetupFutureUsageType>,
    PlaygroundSettingDefinition.Saveable<PaymentMethodOptionsSetupFutureUsageType> by EnumSaveable(
        key = "paymentMethodOptionsSetupFutureUsage",
        values = PaymentMethodOptionsSetupFutureUsageType.entries.toTypedArray(),
        defaultValue = PaymentMethodOptionsSetupFutureUsageType.Off
    ),
    PlaygroundSettingDefinition.Displayable<PaymentMethodOptionsSetupFutureUsageType> {

    override val displayName: String = "Payment Method Options SetupFutureUsage"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = PaymentMethodOptionsSetupFutureUsageType.entries.map {
        option(it.name, it)
    }

    override fun convertToValue(value: String): PaymentMethodOptionsSetupFutureUsageType {
        return PaymentMethodOptionsSetupFutureUsageType.entries.find { it.value == value }
            ?: PaymentMethodOptionsSetupFutureUsageType.Off
    }

    override fun convertToString(value: PaymentMethodOptionsSetupFutureUsageType) = value.value

    override fun configure(
        value: PaymentMethodOptionsSetupFutureUsageType,
        checkoutRequestBuilder: CheckoutRequest.Builder
    ) {
        if (value.valuesMap.isNotEmpty()) {
            checkoutRequestBuilder.paymentMethodOptionsSetupFutureUsage(value.valuesMap)
        }
    }
}

enum class PaymentMethodOptionsSetupFutureUsageType(
    val valuesMap: Map<String, String>
) : ValueEnum {
    All(
        valuesMap = mapOf(
            PaymentMethod.Type.Card.code to "off_session",
            PaymentMethod.Type.USBankAccount.code to "off_session",
            PaymentMethod.Type.CashAppPay.code to "on_session",
            PaymentMethod.Type.AmazonPay.code to "off_session",
            PaymentMethod.Type.Link.code to "off_session",
            PaymentMethod.Type.Affirm.code to "none",
            PaymentMethod.Type.WeChatPay.code to "none"
        )
    ),
    NoneForUnsupported(
        valuesMap = mapOf(
            PaymentMethod.Type.Affirm.code to "none",
            PaymentMethod.Type.WeChatPay.code to "none"
        )
    ),
    OffSessionForCardAndUsBank(
        valuesMap = mapOf(
            PaymentMethod.Type.Card.code to "off_session",
            PaymentMethod.Type.USBankAccount.code to "off_session"
        )
    ),
    Off(valuesMap = emptyMap());

    override val value: String
        get() = valuesMap.entries.joinToString(",") {
            "${it.key}:${it.value}"
        }
}
