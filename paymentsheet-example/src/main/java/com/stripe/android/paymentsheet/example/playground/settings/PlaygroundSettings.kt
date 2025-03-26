package com.stripe.android.paymentsheet.example.playground.settings

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
        val configurationData = updater(_configurationData.value)

        /*
         * Resets value of definitions if the definition's selected option not applicable to the selected
         * integration type to the first available option.
         *
         * For example: Switching from `PaymentSheet` to `CustomerSheet` where the
         * merchant country code value is 'MX'. `CustomerSheet` only supports
         * `US` and `FR`. The value would be reset to the first available option (in
         * this case `US`)
         */
        displayableDefinitions.value.forEach { definition ->
            val values = definition.createOptions(configurationData).map { option ->
                option.value
            }

            if (values.isEmpty()) {
                return@forEach
            }

            val value = settings[definition]?.value

            /*
             * Keeps the existing customer ID if the country value can be shared between integration types
             */
            if (definition == CustomerSettingsDefinition && value is CustomerType.Existing) {
                val countryOptions = CountrySettingsDefinition.createOptions(configurationData)
                val country = settings[CountrySettingsDefinition]?.value

                if (countryOptions.any { it.value == country }) {
                    return@forEach
                }
            }

            if (!values.contains(value)) {
                settings[definition]?.value = values.firstOrNull()
            }
        }

        _configurationData.value = configurationData
    }

    fun snapshot(): Snapshot {
        return Snapshot(this)
    }

    @Stable
    class Snapshot private constructor(
        val configurationData: PlaygroundConfigurationData,
        private val settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            configurationData = parcel.readParcelable<PlaygroundConfigurationData>(
                PlaygroundConfigurationData::class.java.classLoader
            ) ?: throw IllegalStateException("Playground Configuration Data couldn't be un-parceled!"),
            settings = Json.decodeFromString(
                deserializer = MapSerializer(String.serializer(), String.serializer()),
                string = parcel.readString()
                    ?: throw IllegalStateException("Playground Settings couldn't be un-parceled!"),
            ).toPlaygroundSettingsSnapshot()
        )

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
            playgroundState: PlaygroundState.Payment
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

        @ExperimentalEmbeddedPaymentElementApi
        fun embeddedConfiguration(
            playgroundState: PlaygroundState.Payment
        ): EmbeddedPaymentElement.Configuration {
            val builder = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            val embeddedConfigurationData = PlaygroundSettingDefinition.EmbeddedConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, embeddedConfigurationData)
            }
            return builder.build()
        }

        fun customerSheetConfiguration(
            playgroundState: PlaygroundState.Customer
        ): CustomerSheet.Configuration {
            val builder = CustomerSheet.Configuration.builder("Example, Inc.")
            val customerSheetConfigurationData =
                PlaygroundSettingDefinition.CustomerSheetConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, customerSheetConfigurationData)
            }
            return builder.build()
        }

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: PaymentSheet.Configuration.Builder,
            playgroundState: PlaygroundState.Payment,
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

        @ExperimentalEmbeddedPaymentElementApi
        private fun <T> PlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
            playgroundState: PlaygroundState.Payment,
            configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(
                value = value as T,
                configurationBuilder = configurationBuilder,
                playgroundState = playgroundState,
                configurationData = configurationData,
            )
        }

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: CustomerSheet.Configuration.Builder,
            playgroundState: PlaygroundState.Customer,
            configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
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

        fun customerEphemeralKeyRequest(): CustomerEphemeralKeyRequest {
            val builder = CustomerEphemeralKeyRequest.Builder()
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

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder,
            value: Any?,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(value as T, customerEphemeralKeyRequestBuilder)
        }

        private fun asJsonString(filter: (PlaygroundSettingDefinition<*>) -> Boolean): String {
            val settingsMap = settings.filterKeys(filter).mapToStringMap()
            return Json.encodeToString(
                SerializableSettings(
                    configurationData = configurationData,
                    settings = settingsMap,
                )
            )
        }

        fun setValues() {
            settings.forEach { (setting, value) ->
                setting.setValue(value)
            }
        }

        private fun <T> PlaygroundSettingDefinition<T>.setValue(
            value: Any?,
        ) {
            @Suppress("UNCHECKED_CAST")
            (this.setValue(value as T))
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

        private fun Map<PlaygroundSettingDefinition<*>, Any?>.mapToStringMap(): Map<String, String> {
            return map {
                val saveable = it.key.saveable()
                if (saveable != null) {
                    saveable.key to saveable.convertToString(it.value)
                } else {
                    null
                }
            }.filterNotNull().toMap()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(configurationData, flags)
            parcel.writeString(Json.encodeToString(settings.mapToStringMap()))
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Snapshot> {
            override fun createFromParcel(parcel: Parcel): Snapshot {
                return Snapshot(parcel)
            }

            override fun newArray(size: Int): Array<Snapshot?> {
                return arrayOfNulls(size)
            }
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

        private fun Map<String, String>.toPlaygroundSettingsSnapshot(): Map<PlaygroundSettingDefinition<*>, Any?> {
            val settings = mutableMapOf<PlaygroundSettingDefinition<*>, Any?>()

            for (settingDefinition in allSettingDefinitions) {
                settingDefinition.saveable()?.let { saveable ->
                    val value = this[saveable.key]?.let { stringValue ->
                        saveable.convertToValue(stringValue)
                    } ?: saveable.defaultValue

                    settings[settingDefinition] = value
                } ?: run {
                    settings[settingDefinition] = settingDefinition.defaultValue
                }
            }

            return settings
        }

        private val uiSettingDefinitions: List<PlaygroundSettingDefinition.Displayable<*>> = listOf(
            InitializationTypeSettingsDefinition,
            CustomerSheetPaymentMethodModeDefinition,
            CustomerSessionSettingsDefinition,
            CustomerSessionSaveSettingsDefinition,
            CustomerSessionRemoveSettingsDefinition,
            CustomerSessionRemoveLastSettingsDefinition,
            CustomerSessionSetAsDefaultSettingsDefinition,
            CustomerSessionSyncDefaultSettingsDefinition,
            CustomerSessionRedisplaySettingsDefinition,
            CustomerSessionRedisplayFiltersSettingsDefinition,
            CustomerSessionOverrideRedisplaySettingsDefinition,
            FeatureFlagSettingsDefinition(FeatureFlags.editSavedCardPaymentMethodEnabled),
            CustomerSettingsDefinition,
            CheckoutModeSettingsDefinition,
            LinkSettingsDefinition,
            LinkTypeSettingsDefinition,
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
            RequireCvcRecollectionDefinition,
            PrimaryButtonLabelSettingsDefinition,
            PaymentMethodConfigurationSettingsDefinition,
            PreferredNetworkSettingsDefinition,
            AllowsRemovalOfLastSavedPaymentMethodSettingsDefinition,
            PaymentMethodOrderSettingsDefinition,
            ExternalPaymentMethodSettingsDefinition,
            CustomPaymentMethodsSettingDefinition,
            LayoutSettingsDefinition,
            CardBrandAcceptanceSettingsDefinition,
            FeatureFlagSettingsDefinition(FeatureFlags.instantDebitsIncentives),
            FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsLiteKillswitch),
            FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsFullSdkUnavailable),
            EmbeddedViewDisplaysMandateSettingDefinition,
        )

        private val nonUiSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            AppearanceSettingsDefinition,
            CustomEndpointDefinition,
            ShippingAddressSettingsDefinition,
            EmbeddedAppearanceSettingsDefinition
        )

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> =
            uiSettingDefinitions + nonUiSettingDefinitions
    }
}
