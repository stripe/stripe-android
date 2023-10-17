package com.stripe.android.financialconnections.example.settings

import android.content.Context
import androidx.core.content.edit
import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class PlaygroundSettings(
    private val settings: Map<PlaygroundSettingDefinition<*>, Any?>
) {

    val uiSettings: Map<PlaygroundSettingDefinition.Displayable<*>, Any?>
        get() = settings
            .mapNotNull { (def, value) -> def.displayable()?.let { it to value } }
            .toMap()


    fun <T> withValue(
        settingsDefinition: PlaygroundSettingDefinition<T>,
        value: T
    ): PlaygroundSettings {
        val newSettings = copy(settings = settings + (settingsDefinition to value))
        // apply any side effects
        return settingsDefinition.valueUpdated(value, newSettings)
    }

    fun <T> remove(
        settingsDefinition: PlaygroundSettingDefinition<T>
    ): PlaygroundSettings = copy(
        settings = settings - settingsDefinition
    )

    operator fun <T> get(settingsDefinition: PlaygroundSettingDefinition<T>): T {
        @Suppress("UNCHECKED_CAST")
        return settings[settingsDefinition] as T
    }

    fun lasRequest() = settings.toList().fold(
        LinkAccountSessionBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, (definition: PlaygroundSettingDefinition<*>, value: Any?) ->
        definition.lasRequest(acc, value)
    }

    fun paymentIntentRequest() = settings.toList().fold(
        PaymentIntentBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, (definition: PlaygroundSettingDefinition<*>, value: Any?) ->
        definition.paymentIntentRequest(acc, value)
    }


    fun asJsonString(): String {
        val settingsMap = settings.map {
            val saveable = it.key.saveable()
            if (saveable != null) {
                saveable.key to JsonPrimitive(saveable.convertToString(it.value))
            } else {
                null
            }
        }.filterNotNull().toMap()
        return Json.encodeToString(JsonObject(settingsMap))
    }

    fun saveToSharedPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences(
            sharedPreferencesName,
            Context.MODE_PRIVATE
        )

        sharedPreferences.edit {
            putString(
                sharedPreferencesKey,
                asJsonString()
            )
        }
    }

    private fun <T> PlaygroundSettingDefinition.Saveable<T>.convertToString(
        value: Any?,
    ): String {
        @Suppress("UNCHECKED_CAST")
        return convertToString(value as T)
    }

    companion object {

        private const val sharedPreferencesName = "FINANCIAL_CONNECTIONS_DEBUG"
        private const val sharedPreferencesKey = "json"

        fun createFromDefaults(): PlaygroundSettings {
            val settings = allSettingDefinitions.mapNotNull {
                val saveable = it.saveable()
                if (saveable != null) {
                    it to saveable.defaultValue
                } else {
                    null
                }
            }.toMap().toMutableMap()
            return PlaygroundSettings(settings)
        }

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            MerchantDefinition,
            NativeOverrideDefinition,
            FlowDefinition,
            EmailDefinition,
        )
    }

}
