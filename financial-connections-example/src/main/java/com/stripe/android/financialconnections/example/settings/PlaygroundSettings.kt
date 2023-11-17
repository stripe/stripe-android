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
        setting: Setting<T>,
        value: T
    ): PlaygroundSettings = copy(settings = setting.valueUpdated(settings, value))

    inline fun <reified T : Setting<*>> get(): T {
        return settings.find { it is T } as T
    }

    fun lasRequest(): LinkAccountSessionBody = settings.toList().fold(
        LinkAccountSessionBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, setting: Setting<*> -> (setting as Setting<Any?>).lasRequest(acc) }

    fun paymentIntentRequest(): PaymentIntentBody = settings.toList().fold(
        PaymentIntentBody(testEnvironment = BuildConfig.TEST_ENVIRONMENT)
    ) { acc, setting: Setting<*> -> (setting as Setting<Any?>).paymentIntentRequest(acc) }

    fun asJsonString(): String {
        val json = settings
            .mapNotNull { setting ->
                setting.saveable()?.let { it.key to JsonPrimitive(it.convertToString(setting.selectedOption)) }
            }.toMap()
        return Json.encodeToString(JsonObject(json))
    }

    fun asDeeplinkUri(): Uri {
        val builder = Uri.Builder()
            .scheme("stripeconnectionsexample")
            .authority("playground")
        for (definition in settings) {
            val saveable = definition.saveable()
            val value = definition.selectedOption
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
                allSettings
                    .filter { it.selectedOption != null }
                    .toList()
            )
        }

        private fun createFromJsonString(jsonString: String): PlaygroundSettings {
            var settings = PlaygroundSettings(emptyList())
            val jsonObject: JsonObject = Json.decodeFromString(JsonObject.serializer(), jsonString)

            allSettings.map {
                @Suppress("UNCHECKED_CAST")
                it as Setting<Any?>
            }.forEach { setting ->
                val saveable = setting.saveable()
                val savedValue = saveable?.key?.let { jsonObject[it] as? JsonPrimitive }
                if (savedValue?.isString == true) {
                    // add item to mutable list
                    settings = settings.withValue(setting, saveable.convertToValue(savedValue.content))
                } else if (setting.selectedOption != null) {
                    settings = settings.withValue(setting, setting.selectedOption)
                }
            }

            return settings
        }

        fun createFromDeeplinkUri(uri: Uri): PlaygroundSettings {
            var settings = PlaygroundSettings(emptyList())

            allSettings.map {
                @Suppress("UNCHECKED_CAST")
                it as Setting<Any?>
            }.forEach { setting ->
                val saveable = setting.saveable()
                val savedValue: String? = saveable?.key?.let { uri.getQueryParameter(it) }
                if (savedValue != null) {
                    settings = settings.withValue(setting, saveable.convertToValue(savedValue))
                } else if (setting.selectedOption != null) {
                    settings = settings.withValue(setting, setting.selectedOption)
                }
            }

            return settings
        }

        private val allSettings: List<Setting<*>> = listOf(
            MerchantSetting(),
            PublicKeySetting(),
            PrivateKeySetting(),
            FlowSetting(),
            NativeSetting(),
            PermissionsSetting(),
            EmailSetting(),
        )
    }
}
