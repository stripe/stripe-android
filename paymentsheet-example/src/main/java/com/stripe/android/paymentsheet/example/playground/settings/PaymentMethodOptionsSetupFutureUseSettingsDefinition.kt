package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object PaymentMethodOptionsSetupFutureUseSettingsDefinition :
    PlaygroundSettingDefinition<PaymentMethodOptionsSetupFutureUseType>,
    PlaygroundSettingDefinition.Saveable<PaymentMethodOptionsSetupFutureUseType> by EnumSaveable(
        key = "paymentMethodOptionsSetupFutureUse",
        values = PaymentMethodOptionsSetupFutureUseType.entries.toTypedArray(),
        defaultValue = PaymentMethodOptionsSetupFutureUseType.Off
    ),
    PlaygroundSettingDefinition.Displayable<PaymentMethodOptionsSetupFutureUseType> {

    override val displayName: String = "Payment Method Options SetupFutureUse"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = PaymentMethodOptionsSetupFutureUseType.entries.map {
        option(it.name, it)
    }

    override fun convertToValue(value: String): PaymentMethodOptionsSetupFutureUseType {

        return PaymentMethodOptionsSetupFutureUseType.entries.find { it.value == value }
            ?: PaymentMethodOptionsSetupFutureUseType.Off
    }

    override fun convertToString(value: PaymentMethodOptionsSetupFutureUseType) = value.value

    override fun configure(
        value: PaymentMethodOptionsSetupFutureUseType,
        checkoutRequestBuilder: CheckoutRequest.Builder
    ) {
        if (value.valuesMap.isNotEmpty()) {
            checkoutRequestBuilder.paymentMethodOptionsSetupFutureUse(value.valuesMap)
        }
    }
}

enum class PaymentMethodOptionsSetupFutureUseType(
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
