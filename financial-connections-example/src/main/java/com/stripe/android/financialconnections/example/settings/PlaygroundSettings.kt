package com.stripe.android.financialconnections.example.settings

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class PlaygroundSettings(
    val settings: Map<PlaygroundSettingDefinition<*>, Any?>
) {

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

    fun lasRequest(): LinkAccountSessionBody = settings.toList().fold(
        LinkAccountSessionBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, (definition: PlaygroundSettingDefinition<*>, value: Any?) ->
        @Suppress("UNCHECKED_CAST")
        (definition as PlaygroundSettingDefinition<Any?>).lasRequest(acc, value)
    }

    fun paymentIntentRequest(): PaymentIntentBody = settings.toList().fold(
        PaymentIntentBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, (definition: PlaygroundSettingDefinition<*>, value: Any?) ->
        @Suppress("UNCHECKED_CAST")
        (definition as PlaygroundSettingDefinition<Any?>).paymentIntentRequest(acc, value)
    }

    fun asJsonString(): String {
        val settingsMap = settings.map {
            val saveable = it.key
            saveable.key to JsonPrimitive(saveable.convertToString(it.value))
        }.toMap()
        return Json.encodeToString(JsonObject(settingsMap))
    }

    fun saveToSharedPreferences(context: Application) {
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

    private fun <T> PlaygroundSettingDefinition<T>.convertToString(
        value: Any?,
    ): String {
        @Suppress("UNCHECKED_CAST")
        return convertToString(value as T)
    }

    companion object {

        private const val sharedPreferencesName = "FINANCIAL_CONNECTIONS_DEBUG"
        private const val sharedPreferencesKey = "json"

        fun createFromSharedPreferences(context: Application): PlaygroundSettings {
            val sharedPreferences = context.getSharedPreferences(
                sharedPreferencesName,
                Context.MODE_PRIVATE
            )
            return runCatching { sharedPreferences.getString(sharedPreferencesKey, null) }
                .onFailure { Log.e("PlaygroundSettings", "Failed to read from shared preferences", it) }
                .getOrNull()
                ?.let { createFromJsonString(it) }
                ?: createFromDefaults()
        }

        fun createFromDefaults(): PlaygroundSettings {
            val settings = defaultSettingDefinitions
                .associateWith { it.defaultValue }
                .toMutableMap()
            return PlaygroundSettings(settings)
        }

        private fun createFromJsonString(jsonString: String): PlaygroundSettings {
            val settings: MutableMap<PlaygroundSettingDefinition<*>, Any?> = mutableMapOf()
            val jsonObject = Json.decodeFromString(JsonObject.serializer(), jsonString)

            for (definition in allSettingDefinitions) {
                val jsonPrimitive = jsonObject[definition.key] as? JsonPrimitive?
                if (jsonPrimitive?.isString == true) {
                    settings[definition] = definition.convertToValue(jsonPrimitive.content)
                } else {
                    if (defaultSettingDefinitions.contains(definition)) {
                        settings[definition] = definition.defaultValue
                    }
                }
            }

            return PlaygroundSettings(settings)
        }

        private val defaultSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            MerchantDefinition,
            NativeOverrideDefinition,
            FlowDefinition,
            EmailDefinition,
        )

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> = defaultSettingDefinitions + listOf(
            PublicKeyDefinition,
            PrivateKeyDefinition,
        )
    }
}
