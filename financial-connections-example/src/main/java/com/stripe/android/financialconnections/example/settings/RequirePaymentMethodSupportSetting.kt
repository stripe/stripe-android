package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class RequirePaymentMethodSupportSetting(
    override val selectedOption: String? = null,
    override val key: String = "require_payment_method_support",
) : Saveable<String?>, SingleChoiceSetting<String?>(
    displayName = "Require payment method support",
    options = listOf(
        Option("unset", null),
        Option("none", "none"),
        Option("at_least_one", "at_least_one"),
        Option("all", "all"),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        requirePaymentMethodSupport = selectedOption,
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean =
        experience == Experience.FinancialConnections

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String?): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: String?): String = value ?: ""
    override fun convertToValue(value: String): String? = value.takeIf { it.isNotEmpty() }
}
