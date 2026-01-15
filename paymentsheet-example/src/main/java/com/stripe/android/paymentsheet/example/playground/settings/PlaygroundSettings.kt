package com.stripe.android.paymentsheet.example.playground.settings

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.uicore.utils.combineAsStateFlow
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
    initialSettings: Map<PlaygroundSettingDefinition<*>, Any?>
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<Map<PlaygroundSettingDefinition<*>, Any?>> = _settings.asStateFlow()

    private val _configurationData = MutableStateFlow(initialConfigurationData)
    val configurationData = _configurationData.asStateFlow()

    val displayableDefinitions = combineAsStateFlow(_configurationData, settings) { data, settings ->
        settings
            .filterKeys { it.applicable(data, settings) }
            .keys
            .filterIsInstance<PlaygroundSettingDefinition.Displayable<*>>()
    }

    operator fun <T> get(settingsDefinition: PlaygroundSettingDefinition<T>): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return settings.mapAsStateFlow { it[settingsDefinition] as T }
    }

    operator fun <T> set(settingsDefinition: PlaygroundSettingDefinition<T>, value: T) {
        _settings.value += (settingsDefinition to value)
        settingsDefinition.valueUpdated(value, this)
    }

    fun updateConfigurationData(
        updater: (PlaygroundConfigurationData) -> PlaygroundConfigurationData
    ) {
        val configurationData = updater(_configurationData.value)
        var currentSettings = settings.value

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

            val value = currentSettings[definition]

            /*
             * Keeps the existing customer ID if the country value can be shared between integration types
             */
            if (definition == CustomerSettingsDefinition && value is CustomerType.Existing) {
                val countryOptions = MerchantSettingsDefinition.createOptions(configurationData)
                val country = currentSettings[MerchantSettingsDefinition]

                if (countryOptions.any { it.value == country }) {
                    return@forEach
                }
            }

            if (!values.contains(value)) {
                currentSettings = currentSettings + (definition to values.firstOrNull())
            }
        }

        _settings.value = currentSettings
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
            playgroundSettings.settings.value
        )

        operator fun <T> get(settingsDefinition: PlaygroundSettingDefinition<T>): T {
            @Suppress("UNCHECKED_CAST")
            return settings[settingsDefinition] as T
        }

        fun playgroundSettings(): PlaygroundSettings {
            return PlaygroundSettings(configurationData, settings)
        }

        fun paymentSheetConfiguration(
            playgroundState: PlaygroundState.Payment,
            appSettings: Settings,
        ): PaymentSheet.Configuration {
            val builder = PaymentSheet.Configuration.Builder("Example, Inc.")
            val paymentSheetConfigurationData =
                PlaygroundSettingDefinition.PaymentSheetConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition
                    .configure(value, builder, playgroundState, paymentSheetConfigurationData, appSettings)
            }
            return builder.build()
        }

        fun paymentSheetConfiguration(
            playgroundState: PlaygroundState.SharedPaymentToken
        ): PaymentSheet.Configuration {
            val builder = PaymentSheet.Configuration.Builder("Example, Inc.")
            val paymentSheetConfigurationData =
                PlaygroundSettingDefinition.PaymentSheetConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, paymentSheetConfigurationData)
            }
            return builder.build()
        }

        fun embeddedConfiguration(
            playgroundState: PlaygroundState.Payment
        ): EmbeddedPaymentElement.Configuration {
            val builder = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            val embeddedConfigurationData = PlaygroundSettingDefinition.EmbeddedConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, embeddedConfigurationData)
            }
            return builder.build()
        }

        fun linkControllerConfiguration(
            context: Context,
            playgroundState: PlaygroundState.Payment
        ): LinkController.Configuration {
            val paymentConfiguration = PaymentConfiguration.getInstance(context)
            val builder = LinkController.Configuration.Builder(
                merchantDisplayName = "Example, Inc.",
                publishableKey = paymentConfiguration.publishableKey,
                stripeAccountId = paymentConfiguration.stripeAccountId,
            )
            val linkControllerConfigurationData = PlaygroundSettingDefinition.LinkControllerConfigurationData(builder)
            settings.filter { (definition, _) ->
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(value, builder, playgroundState, linkControllerConfigurationData)
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
                definition.applicable(configurationData, settings)
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
            settings: Settings,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(
                value = value as T,
                configurationBuilder = configurationBuilder,
                playgroundState = playgroundState,
                configurationData = configurationData,
                settings = settings,
            )
        }

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
            configurationBuilder: LinkController.Configuration.Builder,
            playgroundState: PlaygroundState.Payment,
            configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
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

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            value: Any?,
            configurationBuilder: PaymentSheet.Configuration.Builder,
            playgroundState: PlaygroundState.SharedPaymentToken,
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
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(builder, value, settings)
            }
            return builder.build()
        }

        fun customerEphemeralKeyRequest(): CustomerEphemeralKeyRequest {
            val builder = CustomerEphemeralKeyRequest.Builder()
            settings.filter { (definition, _) ->
                definition.applicable(configurationData, settings)
            }.onEach { (settingDefinition, value) ->
                settingDefinition.configure(builder, value)
            }
            return builder.build()
        }

        private fun <T> PlaygroundSettingDefinition<T>.configure(
            checkoutRequestBuilder: CheckoutRequest.Builder,
            value: Any?,
            settings: Map<PlaygroundSettingDefinition<*>, Any?>,
        ) {
            @Suppress("UNCHECKED_CAST")
            configure(value as T, checkoutRequestBuilder, settings)
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
                settingDefinition.defaultValue
            }
            return PlaygroundSettings(defaultConfigurationData, settings)
        }

        fun createFromJsonString(jsonString: String): PlaygroundSettings {
            val unserializedSettings = try {
                Json.decodeFromString(SerializableSettings.serializer(), jsonString)
            } catch (exception: SerializationException) {
                Log.e("PlaygroundParsingError", "Error parsing settings from string", exception)

                return createFromDefaults()
            }

            val settings = allSettingDefinitions.associateWith { settingDefinition ->
                settingDefinition.saveable()?.let { saveable ->
                    unserializedSettings.settings[saveable.key]?.let { stringValue ->
                        saveable.convertToValue(stringValue)
                    } ?: saveable.defaultValue
                } ?: settingDefinition.defaultValue
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

        val uiSettingDefinitions: List<PlaygroundSettingDefinition.Displayable<*>> = listOf(
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
            CustomerSessionOnBehalfOfSettingsDefinition,
            CustomerSettingsDefinition,
            CheckoutModeSettingsDefinition,
            UserCountryOverrideSettingsDefinition,
            LinkSettingsDefinition,
            LinkTypeSettingsDefinition,
            MerchantSettingsDefinition,
            CustomSecretKeyDefinition,
            CustomPublishableKeyDefinition,
            CurrencySettingsDefinition,
            GooglePaySettingsDefinition,
            GooglePayCustomerSheetSettingsDefinition,
            DefaultBillingAddressSettingsDefinition,
            AttachBillingDetailsToPaymentMethodSettingsDefinition,
            CollectNameSettingsDefinition,
            CollectEmailSettingsDefinition,
            CollectPhoneSettingsDefinition,
            CollectAddressSettingsDefinition,
            AllowedBillingCountriesSettingsDefinition,
            AutocompleteAddressSettingsDefinition,
            DefaultShippingAddressSettingsDefinition,
            DelayedPaymentMethodsSettingsDefinition,
            AutomaticPaymentMethodsSettingsDefinition,
            SupportedPaymentMethodsSettingsDefinition,
            AutomaticallyLaunchCardScanDefinition,
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
            CardFundingAcceptanceSettingsDefinition,
            FeatureFlagSettingsDefinition(
                FeatureFlags.enableKlarnaFormRemoval,
                listOf(
                    PlaygroundConfigurationData.IntegrationType.PaymentSheet,
                    PlaygroundConfigurationData.IntegrationType.FlowController,
                    PlaygroundConfigurationData.IntegrationType.Embedded,
                ),
            ),
            FeatureFlagSettingsDefinition(
                FeatureFlags.instantDebitsIncentives,
                PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
            ),
            FeatureFlagSettingsDefinition(
                FeatureFlags.financialConnectionsFullSdkUnavailable,
                PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
            ),
            FeatureFlagSettingsDefinition(FeatureFlags.forceEnableNativeFinancialConnections),
            EmbeddedViewDisplaysMandateSettingDefinition,
            EmbeddedFormSheetActionSettingDefinition,
            EmbeddedTwoStepSettingsDefinition,
            EmbeddedRowSelectionBehaviorSettingsDefinition,
            PaymentMethodOptionsSetupFutureUsageSettingsDefinition,
            PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition,
            WalletButtonsSettingsDefinition,
            FeatureFlagSettingsDefinition(
                FeatureFlags.showInlineOtpInWalletButtons,
                allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList() +
                    PlaygroundConfigurationData.IntegrationType.sptFlows().toList(),
            ),
            ShopPaySettingsDefinition,
            LinkControllerAllowUserEmailEditsSettingsDefinition,
            FeatureFlagSettingsDefinition(FeatureFlags.forceLinkWebAuth),
            FeatureFlagSettingsDefinition(
                FeatureFlags.forceEnableLinkPaymentSelectionHint,
                listOf(PlaygroundConfigurationData.IntegrationType.LinkController)
            ),
            TermsDisplaySettingsDefinition,
            PassiveCaptchaDefinition,
            AttestationOnIntentConfirmationDefinition,
            EnableTapToAddSettingsDefinition,
            CustomStripeApiDefinition,
        )

        private val nonUiSettingDefinitions: List<PlaygroundSettingDefinition<*>> = listOf(
            AppearanceSettingsDefinition,
            CustomEndpointDefinition,
            ShippingAddressSettingsDefinition,
            ConfirmationTokenSettingsDefinition,
        )

        private val allSettingDefinitions: List<PlaygroundSettingDefinition<*>> =
            uiSettingDefinitions + nonUiSettingDefinitions
    }
}
