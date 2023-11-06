package com.stripe.android.financialconnections.example.settings

import android.app.Application
import android.content.Context
import android.net.Uri
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
    val settings: List<Setting<*>>
) {

    fun <T> withValue(
        settingsDefinition: Setting<T>,
        value: T
    ): PlaygroundSettings = copy(settings = settingsDefinition.valueUpdated(settings, value))

    inline fun <reified T : Setting<*>> get(): T {
        return settings.find { it is T } as T
    }

    fun lasRequest(): LinkAccountSessionBody = settings.toList().fold(
        LinkAccountSessionBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, definition: Setting<*> -> (definition as Setting<Any?>).lasRequest(acc) }

    fun paymentIntentRequest(): PaymentIntentBody = settings.toList().fold(
        PaymentIntentBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, definition: Setting<*> -> (definition as Setting<Any?>).paymentIntentRequest(acc) }


    fun asJsonString(): String {
        val json = settings
            .mapNotNull { setting ->
                setting.saveable()?.let { it.key to JsonPrimitive(it.convertToString(setting.selectedOption)) }
            }.toMap()
        return Json.encodeToString(JsonObject(json))
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

    private fun <T> Saveable<T>.convertToString(
        value: Any?,
    ): String? {
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
            return PlaygroundSettings(
                allSettingDefinitions
                    .filter { it.selectedOption != null }
                    .toList())
        }

        private fun createFromJsonString(jsonString: String): PlaygroundSettings {
            var settings = PlaygroundSettings(emptyList())
            val jsonObject: JsonObject = Json.decodeFromString(JsonObject.serializer(), jsonString)

            for (definition in allSettingDefinitions) {
                val saveable = definition.saveable()
                val savedValue = saveable?.key?.let { jsonObject[it] as? JsonPrimitive }
                @Suppress("UNCHECKED_CAST")
                val settingsDefinition = definition as Setting<Any?>
                if (savedValue?.isString == true) {
                    // add item to mutable list
                    settings = settings.withValue(settingsDefinition, saveable.convertToValue(savedValue.content))
                } else if (definition.selectedOption != null) {
                    settings = settings.withValue(settingsDefinition, definition.selectedOption)
                }
            }

            return settings
        }

        fun createFromDeeplinkUri(uri: Uri): PlaygroundSettings {
            var settings = PlaygroundSettings(emptyList())

            for (definition in allSettingDefinitions) {
                val saveable = definition.saveable()
                val savedValue: String? = saveable?.key?.let { uri.getQueryParameter(it) }
                @Suppress("UNCHECKED_CAST")
                val settingsDefinition = definition as Setting<Any?>
                if (savedValue != null) {
                    settings = settings.withValue(settingsDefinition, savedValue)
                } else if (definition.selectedOption != null) {
                    settings = settings.withValue(settingsDefinition, definition.selectedOption)
                }
            }

            return settings
        }

        private val allSettingDefinitions: List<Setting<*>> = listOf(
            MerchantSetting(),
            FlowSetting(),
            NativeSetting(),
            PublicKeySetting(),
            PrivateKeySetting(),
            EmailDefinition(),
        )
    }
}
