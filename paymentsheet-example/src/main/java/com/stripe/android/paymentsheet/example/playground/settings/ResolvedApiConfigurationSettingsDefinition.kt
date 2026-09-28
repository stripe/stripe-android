package com.stripe.android.paymentsheet.example.playground.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object ResolvedApiConfigurationSettingsDefinition :
    PlaygroundSettingDefinition<ResolvedApiConfiguration>,
    PlaygroundSettingDefinition.Saveable<ResolvedApiConfiguration> {
    override val key: String = "resolvedApiConfiguration"
    override val defaultValue: ResolvedApiConfiguration = ResolvedApiConfiguration()
    override val saveToSharedPreferences: Boolean = false

    override fun convertToString(value: ResolvedApiConfiguration): String {
        return Json.encodeToString(ResolvedApiConfiguration.serializer(), value)
    }

    override fun convertToValue(value: String): ResolvedApiConfiguration {
        return Json.decodeFromString(ResolvedApiConfiguration.serializer(), value)
    }
}

@Serializable
internal data class ResolvedApiConfiguration(
    val publishableKey: String? = null,
    val stripeAccountId: String? = null,
)
