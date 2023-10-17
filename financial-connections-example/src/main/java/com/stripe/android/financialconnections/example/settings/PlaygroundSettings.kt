package com.stripe.android.financialconnections.example.settings

import android.content.Context
import androidx.core.content.edit
import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.Merchant
import com.stripe.android.financialconnections.example.NativeOverride
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class PlaygroundSettings private constructor(
    val settings: MutableMap<PlaygroundSettingDefinition<*>, MutableStateFlow<Any?>>
) {

    val uiSettings: Map<PlaygroundSettingDefinition.Displayable<*>, MutableStateFlow<Any?>> =
        settings
        .mapNotNull { (def, value) -> def.displayable()?.let { it to value } }
        .toMap()

    operator fun <T> get(settingsDefinition: PlaygroundSettingDefinition<T>): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return settings[settingsDefinition]?.asStateFlow() as StateFlow<T>
    }

    operator fun <T> set(settingsDefinition: PlaygroundSettingDefinition<T>, value: T) {
        if (settings.containsKey(settingsDefinition)) {
            settings[settingsDefinition]?.value = value
        } else {
            settings[settingsDefinition] = MutableStateFlow(value)
        }
    }

    fun toBackendRequest(customerEmail: String?) = settings.toList().fold(
        LinkAccountSessionBody(
            customerEmail = customerEmail,
            testEnvironment = BuildConfig.TEST_ENVIRONMENT,
        )
    ) { acc, (definition: PlaygroundSettingDefinition<*>, value: MutableStateFlow<Any?>) ->
        definition.sessionRequest(acc, value.value)
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

    private fun asJsonString(): String {
        val settingsMap = settings.map {
            val saveable = it.key.saveable()
            if (saveable != null) {
                saveable.key to JsonPrimitive(saveable.convertToString(it.value.value))
            } else {
                null
            }
        }.filterNotNull().toMap()
        return Json.encodeToString(JsonObject(settingsMap))
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

        fun default() = PlaygroundSettings(
            mutableMapOf(
                MerchantDefinition() to MutableStateFlow(Merchant.Test),
                NativeOverrideDefinition() to MutableStateFlow(NativeOverride.None),
                FlowDefinition() to MutableStateFlow(Flow.PaymentIntent),
            )
        )
    }

}
