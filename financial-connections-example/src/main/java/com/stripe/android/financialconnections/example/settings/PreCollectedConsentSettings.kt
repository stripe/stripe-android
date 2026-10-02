package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal enum class PreCollectedConsentMode(val displayName: String) {
    Off("Off"),
    Guided("Guided"),
    Manual("Manual");

    companion object {
        fun fromApiValue(value: String): PreCollectedConsentMode = entries.first { it.name == value }
    }
}

internal data class PreCollectedConsentModeSetting(
    override val selectedOption: PreCollectedConsentMode = PreCollectedConsentMode.Off,
    override val key: String = "pre_collected_consent_mode",
) : Saveable<PreCollectedConsentMode>, SingleChoiceSetting<PreCollectedConsentMode>(
    displayName = "Pre-collected consent",
    options = PreCollectedConsentMode.entries.map { Option(it.displayName, it) },
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean {
        return experience == Experience.FinancialConnections
    }

    override fun convertToString(value: PreCollectedConsentMode): String = value.name

    override fun convertToValue(value: String): PreCollectedConsentMode =
        PreCollectedConsentMode.fromApiValue(value)

    override fun valueUpdated(
        currentSettings: List<Setting<*>>,
        value: PreCollectedConsentMode,
    ): List<Setting<*>> = replace(currentSettings, copy(selectedOption = value))
}

internal data class PreCollectedConsentLocaleSetting(
    override val selectedOption: String = "",
    override val key: String = "pre_collected_consent_locale",
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "Consent locale",
    options = emptyList(),
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: String): String = value

    override fun convertToValue(value: String): String = value

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String): List<Setting<*>> =
        replace(currentSettings, copy(selectedOption = value))
}

internal data class ManualConsentIdSetting(
    override val selectedOption: String = "",
    override val key: String = "manual_consent_id",
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "Consent ID",
    options = emptyList(),
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: String): String = value

    override fun convertToValue(value: String): String = value

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String): List<Setting<*>> =
        replace(currentSettings, copy(selectedOption = value))
}

internal data class ManualConsentCollectedAtSetting(
    override val selectedOption: String = "",
    override val key: String = "manual_consent_collected_at",
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "Consent collected at (Unix seconds)",
    options = emptyList(),
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: String): String = value

    override fun convertToValue(value: String): String = value

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String): List<Setting<*>> =
        replace(currentSettings, copy(selectedOption = value))
}
