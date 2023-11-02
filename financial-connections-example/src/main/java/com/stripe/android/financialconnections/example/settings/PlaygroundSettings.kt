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
            val saveable = it.key.saveable()
            if (saveable != null) {
                saveable.key to JsonPrimitive(saveable.convertToString(it.value))
            } else {
                null
            }
        }.filterNotNull().toMap()
        return Json.encodeToString(JsonObject(settingsMap))
    }

    fun asDeeplinkUri(): Uri {
        val builder = Uri.Builder()
            .scheme("stripeconnectionsexample")
            .authority("playground")
        for (definition in allSettingDefinitions) {
            val saveable = definition.saveable()
            val value = settings[definition]
            if (saveable != null && value != null) {
                builder.appendQueryParameter(saveable.key, saveable.convertToString(value))
            }
        }
        return builder.build()
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

    private fun <T> PlaygroundSettingDefinition.Saveable<T>.convertToString(
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
            val settings = allSettingDefinitions
                .filter { it.defaultValue != null }
                .associateWith { settingDefinition -> settingDefinition.defaultValue }
                .toMutableMap()
            return PlaygroundSettings(settings)
        }

        private fun createFromJsonString(jsonString: String): PlaygroundSettings {
            val settings: MutableMap<PlaygroundSettingDefinition<*>, Any?> = mutableMapOf()
            val jsonObject: JsonObject = Json.decodeFromString(JsonObject.serializer(), jsonString)

            for (definition in allSettingDefinitions) {
                val saveable = definition.saveable()
                val savedValue = saveable?.key?.let { jsonObject[it] as? JsonPrimitive }
                if (savedValue?.isString == true) {
                    settings[definition] = saveable.convertToValue(savedValue.content)
                } else if (definition.defaultValue != null) {
                    settings[definition] = definition.defaultValue
                }
            }

            return PlaygroundSettings(settings)
        }

        fun createFromDeeplinkUri(uri: Uri): PlaygroundSettings {
            val settings: MutableMap<PlaygroundSettingDefinition<*>, Any?> = mutableMapOf()

            for (definition in allSettingDefinitions) {
                val saveable = definition.saveable()
                val savedValue: String? = saveable?.key?.let { uri.getQueryParameter(it) }
                if (savedValue != null) {
                    settings[definition] = saveable.convertToValue(savedValue)
                } else if (definition.defaultValue != null) {
                    settings[definition] = definition.defaultValue
                }
            }

            return PlaygroundSettings(settings)
        }

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            MerchantDefinition,
            NativeOverrideDefinition,
            FlowDefinition,
            EmailDefinition,
            PublicKeyDefinition,
            PrivateKeyDefinition,
        )
    }
}
