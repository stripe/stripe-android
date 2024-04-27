package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class CustomerSheetPlaygroundSettings private constructor(
    private val settings: MutableMap<CustomerSheetPlaygroundSettingDefinition<*>, MutableStateFlow<Any?>>
) {
    operator fun <T> get(settingsDefinition: CustomerSheetPlaygroundSettingDefinition<T>): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return settings[settingsDefinition]?.asStateFlow() as StateFlow<T>
    }

    operator fun <T> set(settingsDefinition: CustomerSheetPlaygroundSettingDefinition<T>, value: T) {
        if (settings.containsKey(settingsDefinition)) {
            settings[settingsDefinition]?.value = value
        } else {
            settings[settingsDefinition] = MutableStateFlow(value)
        }
        settingsDefinition.valueUpdated(value, this)
    }

    fun snapshot(): Snapshot {
        return Snapshot(this)
    }

    @Stable
    class Snapshot private constructor(
        private val settings: Map<CustomerSheetPlaygroundSettingDefinition<*>, Any?>
    ) {
        constructor(playgroundSettings: CustomerSheetPlaygroundSettings) : this(
            playgroundSettings.settings.map { it.key to it.value.value }.toMap()
        )

        operator fun <T> get(settingsDefinition: CustomerSheetPlaygroundSettingDefinition<T>): T {
            @Suppress("UNCHECKED_CAST")
            return settings[settingsDefinition] as T
        }

        fun playgroundSettings(): CustomerSheetPlaygroundSettings {
            val mutableSettings = settings.map {
                it.key to MutableStateFlow(it.value)
            }.toMap().toMutableMap()
            return CustomerSheetPlaygroundSettings(mutableSettings)
        }

        @OptIn(ExperimentalCustomerSheetApi::class)
        fun customerSheetConfiguration(
            playgroundState: CustomerSheetPlaygroundState
        ): CustomerSheet.Configuration {
            val builder = CustomerSheet.Configuration.builder("Example, Inc.")

            val configurationData =
                CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData(builder)

            settings.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, configurationData)
            }
            return builder.build()
        }

        @OptIn(ExperimentalCustomerSheetApi::class)
        private fun <T> CustomerSheetPlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: CustomerSheet.Configuration.Builder,
            playgroundState: CustomerSheetPlaygroundState,
            configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(
                value = value as T,
                configurationBuilder = configurationBuilder,
                playgroundState = playgroundState,
                configurationData = configurationData,
            )
        }

        fun customerEphemeralKeyRequest(): CustomerEphemeralKeyRequest {
            val builder = CustomerEphemeralKeyRequest.Builder()
            settings.onEach { (settingDefinition, value) ->
                settingDefinition.configure(builder, value)
            }
            return builder.build()
        }

        private fun <T> CustomerSheetPlaygroundSettingDefinition<T>.configure(
            requestBuilder: CustomerEphemeralKeyRequest.Builder,
            value: Any?,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(value as T, requestBuilder)
        }

        private fun asJsonString(filter: (CustomerSheetPlaygroundSettingDefinition<*>) -> Boolean): String {
            val settingsMap = settings.filterKeys(filter).map {
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
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

            sharedPreferences.edit {
                putString(
                    SHARED_PREFERENCES_KEY,
                    asJsonString(filter = { it.saveable()?.saveToSharedPreferences == true })
                )
            }
        }

        fun asJsonString(): String {
            return asJsonString { true }
        }

        private fun <T> CustomerSheetPlaygroundSettingDefinition.Saveable<T>.convertToString(
            value: Any?,
        ): String {
            @Suppress("UNCHECKED_CAST")
            return convertToString(value as T)
        }
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "CustomerSheetPlaygroundSettings"
        private const val SHARED_PREFERENCES_KEY = "json"

        fun createFromDefaults(): CustomerSheetPlaygroundSettings {
            val settings = allSettingDefinitions.associateWith { settingDefinition ->
                MutableStateFlow(settingDefinition.defaultValue)
            }.toMutableMap()
            return CustomerSheetPlaygroundSettings(settings)
        }

        fun createFromJsonString(jsonString: String): CustomerSheetPlaygroundSettings {
            val settings: MutableMap<CustomerSheetPlaygroundSettingDefinition<*>, MutableStateFlow<Any?>> =
                mutableMapOf()
            val jsonObject = Json.decodeFromString(JsonObject.serializer(), jsonString)

            for (settingDefinition in allSettingDefinitions) {
                val saveable = settingDefinition.saveable()
                if (saveable != null) {
                    val jsonPrimitive = jsonObject[saveable.key] as? JsonPrimitive?
                    if (jsonPrimitive?.isString == true) {
                        settings[settingDefinition] =
                            MutableStateFlow(saveable.convertToValue(jsonPrimitive.content))
                    } else {
                        settings[settingDefinition] = MutableStateFlow(settingDefinition.defaultValue)
                    }
                } else {
                    settings[settingDefinition] = MutableStateFlow(settingDefinition.defaultValue)
                }
            }

            return CustomerSheetPlaygroundSettings(settings)
        }

        fun createFromSharedPreferences(context: Context): CustomerSheetPlaygroundSettings {
            val sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

            val jsonString = sharedPreferences.getString(SHARED_PREFERENCES_KEY, null)
                ?: return createFromDefaults()

            return createFromJsonString(jsonString)
        }

        val uiSettingDefinitions: List<CustomerSheetPlaygroundSettingDefinition.Displayable<*>> = listOf(
            CustomerSettingsDefinition,
            CountrySettingsDefinition,
            PaymentMethodModeDefinition,
            GooglePaySettingsDefinition,
            AttachDefaultBillingDetailsDefinition,
            PreferredNetworkSettingsDefinition,
            AllowsRemovalOfLastSavedPaymentMethodSettingsDefinition,
            CollectNameSettingsDefinition,
            CollectEmailSettingsDefinition,
            CollectPhoneSettingsDefinition,
            CollectAddressSettingsDefinition,
        )

        private val nonUiSettingDefinitions: List<CustomerSheetPlaygroundSettingDefinition<*>> = listOf(
            AppearanceSettingsDefinition,
        )

        private val allSettingDefinitions: List<CustomerSheetPlaygroundSettingDefinition<*>> =
            uiSettingDefinitions + nonUiSettingDefinitions
    }
}
