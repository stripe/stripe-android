package com.stripe.android.paymentsheet.example.playground.settings

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class PlaygroundSettings private constructor(
    initialConfigurationData: PlaygroundConfigurationData,
    private val settings: MutableMap<PlaygroundSettingDefinition<*>, MutableStateFlow<Any?>>
) {
    private val _configurationData = MutableStateFlow(initialConfigurationData)
    val configurationData = _configurationData.asStateFlow()

    val displayableDefinitions = _configurationData.mapAsStateFlow { data ->
        settings
            .filterKeys { it.applicable(data) }
            .map { (definition, _) -> definition }
            .filterIsInstance<PlaygroundSettingDefinition.Displayable<*>>()
    }

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

    fun updateConfigurationData(
        updater: (PlaygroundConfigurationData) -> PlaygroundConfigurationData
    ) {
        _configurationData.value = updater(_configurationData.value)
    }

    fun snapshot(): Snapshot {
        return Snapshot(this)
    }

    @Stable
    class Snapshot private constructor(
        val configurationData: PlaygroundConfigurationData,
        private val settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ) {
        constructor(playgroundSettings: PlaygroundSettings) : this(
            playgroundSettings.configurationData.value,
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
            return PlaygroundSettings(configurationData, mutableSettings)
        }

        fun paymentSheetConfiguration(
            playgroundState: PlaygroundState
        ): PaymentSheet.Configuration {
            val builder = PaymentSheet.Configuration.Builder("Example, Inc.")
            val paymentSheetConfigurationData =
                PlaygroundSettingDefinition.PaymentSheetConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, paymentSheetConfigurationData)
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
            settings.filter { (definition, _) ->
                definition.applicable(configurationData)
            }.onEach { (settingDefinition, value) ->
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
                    saveable.key to saveable.convertToString(it.value)
                } else {
                    null
                }
            }.filterNotNull().toMap()
            return Json.encodeToString(
                SerializableSettings(
                    configurationData = configurationData,
                    settings = settingsMap,
                )
            )
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

    @Serializable
    private class SerializableSettings(
        val configurationData: PlaygroundConfigurationData,
        val settings: Map<String, String>,
    )

    companion object {
        private const val sharedPreferencesName = "PlaygroundSettings"
        private const val sharedPreferencesKey = "json"

        fun createFromDefaults(): PlaygroundSettings {
            val defaultConfigurationData = PlaygroundConfigurationData()
            val settings = allSettingDefinitions.associateWith { settingDefinition ->
                MutableStateFlow(settingDefinition.defaultValue)
            }.toMutableMap()
            return PlaygroundSettings(defaultConfigurationData, settings)
        }

        fun createFromJsonString(jsonString: String): PlaygroundSettings {
            val settings: MutableMap<PlaygroundSettingDefinition<*>, MutableStateFlow<Any?>> = mutableMapOf()

            val unserializedSettings = try {
                Json.decodeFromString(SerializableSettings.serializer(), jsonString)
            } catch (exception: SerializationException) {
                Log.e("PlaygroundParsingError", "Error parsing settings from string", exception)

                return createFromDefaults()
            }

            for (settingDefinition in allSettingDefinitions) {
                settingDefinition.saveable()?.let { saveable ->
                    val value = unserializedSettings.settings[saveable.key]?.let { stringValue ->
                        saveable.convertToValue(stringValue)
                    } ?: saveable.defaultValue

                    settings[settingDefinition] = MutableStateFlow(value)
                } ?: run {
                    settings[settingDefinition] = MutableStateFlow(settingDefinition.defaultValue)
                }
            }

            return PlaygroundSettings(unserializedSettings.configurationData, settings)
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

        private val uiSettingDefinitions: List<PlaygroundSettingDefinition.Displayable<*>> = listOf(
            InitializationTypeSettingsDefinition,
            CustomerSheetPaymentMethodModeDefinition,
            CustomerSessionSettingsDefinition,
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
            SupportedPaymentMethodsSettingsDefinition,
            PrimaryButtonLabelSettingsDefinition,
            PaymentMethodConfigurationSettingsDefinition,
            PreferredNetworkSettingsDefinition,
            AllowsRemovalOfLastSavedPaymentMethodSettingsDefinition,
            PaymentMethodOrderSettingsDefinition,
            ExternalPaymentMethodSettingsDefinition,
            LayoutSettingsDefinition,
        )

        private val nonUiSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            AppearanceSettingsDefinition,
            ShippingAddressSettingsDefinition,
        )

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> =
            uiSettingDefinitions + nonUiSettingDefinitions
    }
}
