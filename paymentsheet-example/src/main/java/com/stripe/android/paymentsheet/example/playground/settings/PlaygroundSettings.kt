package com.stripe.android.paymentsheet.example.playground.settings

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class PlaygroundSettings private constructor(
    private val settings: MutableMap<PlaygroundSettingDefinition<*>, MutableStateFlow<Any?>>
) {
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
        settingsDefinition.valueUpdated(value, this)
    }

    fun snapshot(): Snapshot {
        return Snapshot(this)
    }

    @Stable
    class Snapshot private constructor(
        private val settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ) {
        constructor(playgroundSettings: PlaygroundSettings) : this(
            playgroundSettings.settings.map { it.key to it.value.value }.toMap()
        )

        operator fun <T> get(settingsDefinition: PlaygroundSettingDefinition<T>): T {
            @Suppress("UNCHECKED_CAST")
            return settings[settingsDefinition] as T
        }

        fun playgroundSettings(): PlaygroundSettings {
            val mutableSettings = settings.map {
                it.key to MutableStateFlow(it.value)
            }.toMap().toMutableMap()
            return PlaygroundSettings(mutableSettings)
        }

        fun paymentSheetConfiguration(
            playgroundState: PlaygroundState
        ): PaymentSheet.Configuration {
            val builder = PaymentSheet.Configuration.Builder("Example, Inc.")
            val configurationData =
                PlaygroundSettingDefinition.PaymentSheetConfigurationData(builder)
            settings.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, configurationData)
            }
            return builder.build()
        }

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: PaymentSheet.Configuration.Builder,
            playgroundState: PlaygroundState,
            configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(
                value = value as T,
                configurationBuilder = configurationBuilder,
                playgroundState = playgroundState,
                configurationData = configurationData,
            )
        }

        fun checkoutRequest(): CheckoutRequest {
            val builder = CheckoutRequest.Builder()
            settings.onEach { (settingDefinition, value) ->
                settingDefinition.configure(builder, value)
            }
            return builder.build()
        }

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            checkoutRequestBuilder: CheckoutRequest.Builder,
            value: Any?,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(value as T, checkoutRequestBuilder)
        }

        private fun asJsonString(filter: (PlaygroundSettingDefinition<*>) -> Boolean): String {
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
                sharedPreferencesName,
                Context.MODE_PRIVATE
            )

            sharedPreferences.edit {
                putString(
                    sharedPreferencesKey,
                    asJsonString(filter = { it.saveable()?.saveToSharedPreferences == true })
                )
            }
        }

        fun asJsonString(): String {
            return asJsonString { true }
        }

        private fun <T> PlaygroundSettingDefinition.Saveable<T>.convertToString(
            value: Any?,
        ): String {
            @Suppress("UNCHECKED_CAST")
            return convertToString(value as T)
        }
    }

    companion object {
        private const val sharedPreferencesName = "PlaygroundSettings"
        private const val sharedPreferencesKey = "json"

        fun createFromDefaults(): PlaygroundSettings {
            val settings = allSettingDefinitions.associateWith { settingDefinition ->
                MutableStateFlow(settingDefinition.defaultValue)
            }.toMutableMap()
            return PlaygroundSettings(settings)
        }

        fun createFromJsonString(jsonString: String): PlaygroundSettings {
            val settings: MutableMap<PlaygroundSettingDefinition<*>, MutableStateFlow<Any?>> =
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

            return PlaygroundSettings(settings)
        }

        fun createFromSharedPreferences(context: Context): PlaygroundSettings {
            val sharedPreferences = context.getSharedPreferences(
                sharedPreferencesName,
                Context.MODE_PRIVATE
            )

            val jsonString = sharedPreferences.getString(sharedPreferencesKey, null)
                ?: return createFromDefaults()

            return createFromJsonString(jsonString)
        }

        val uiSettingDefinitions: List<PlaygroundSettingDefinition.Displayable<*>> = listOf(
            InitializationTypeSettingsDefinition,
            CustomerSettingsDefinition,
            CheckoutModeSettingsDefinition,
            LinkSettingsDefinition,
            CountrySettingsDefinition,
            CurrencySettingsDefinition,
            GooglePaySettingsDefinition,
            DefaultBillingAddressSettingsDefinition,
            AttachBillingDetailsToPaymentMethodSettingsDefinition,
            CollectNameSettingsDefinition,
            CollectEmailSettingsDefinition,
            CollectPhoneSettingsDefinition,
            CollectAddressSettingsDefinition,
            DefaultShippingAddressSettingsDefinition,
            DelayedPaymentMethodsSettingsDefinition,
            AutomaticPaymentMethodsSettingsDefinition,
            PrimaryButtonLabelSettingsDefinition,
            PreferredNetworkSettingsDefinition,
            IntegrationTypeSettingsDefinition,
        )

        private val nonUiSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            SupportedPaymentMethodsSettingsDefinition,
            AppearanceSettingsDefinition,
            ShippingAddressSettingsDefinition,
        )

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> =
            uiSettingDefinitions + nonUiSettingDefinitions
    }
}
