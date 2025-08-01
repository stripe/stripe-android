package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class ElementsSessionJsonParser(
    private val params: ElementsSessionParams,
    private val isLiveMode: Boolean,
    private val timeProvider: () -> Long = {
        System.currentTimeMillis()
    }
) : ModelJsonParser<ElementsSession> {

    override fun parse(json: JSONObject): ElementsSession? {
        val paymentMethodPreference = StripeJsonUtils.mapToJsonObject(
            StripeJsonUtils.optMap(json, FIELD_PAYMENT_METHOD_PREFERENCE)
        )
        val objectType = StripeJsonUtils.optString(paymentMethodPreference, FIELD_OBJECT)

        if (paymentMethodPreference == null || FIELD_PAYMENT_METHOD_PREFERENCE != objectType) {
            return null
        }

        val countryCode = paymentMethodPreference.optString(FIELD_COUNTRY_CODE)
        val unactivatedPaymentMethodTypes = json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES)
        val paymentMethodSpecs = json.optJSONArray(FIELD_PAYMENT_METHOD_SPECS)?.toString()
        val externalPaymentMethodData = json.optJSONArray(FIELD_EXTERNAL_PAYMENT_METHOD_DATA)?.toString()

        val orderedPaymentMethodTypes =
            paymentMethodPreference.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES)

        val orderedPaymentMethodTypesAndWallets =
            jsonArrayToList(json.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPESAND_WALLETS))

        val flags = json.optJSONObject(FIELD_FLAGS)?.let { flags -> parseSessionFlags(json = flags) } ?: emptyMap()

        val elementsSessionId = json.optString(FIELD_ELEMENTS_SESSION_ID)
        val customer = parseCustomer(
            json = json.optJSONObject(FIELD_CUSTOMER),
            enableLinkInSpm = flags[ElementsSession.Flag.ELEMENTS_ENABLE_LINK_SPM] == true
        )

        val linkSettings = json.optJSONObject(FIELD_LINK_SETTINGS)
        val linkFundingSources = linkSettings?.optJSONArray(FIELD_LINK_FUNDING_SOURCES)

        val stripeIntent = parseStripeIntent(
            elementsSessionId = elementsSessionId,
            paymentMethodPreference = paymentMethodPreference,
            orderedPaymentMethodTypes = orderedPaymentMethodTypes,
            unactivatedPaymentMethodTypes = unactivatedPaymentMethodTypes,
            linkFundingSources = linkFundingSources,
            countryCode = countryCode
        )

        val experimentsData: ElementsSession.ExperimentsData? =
            json.optJSONObject(FIELD_EXPERIMENTS_DATA)?.let { experimentsDataJson ->
                ElementsSession.ExperimentsData(
                    arbId = experimentsDataJson.optString(ARB_ID),
                    experimentAssignments = experimentsDataJson.optJSONObject(FIELD_EXPERIMENTS_ASSIGNMENTS)?.let {
                        parseExperimentAssignments(it)
                    } ?: emptyMap()
                )
            }

        val customPaymentMethods = parseCustomPaymentMethods(json.optJSONArray(FIELD_CUSTOM_PAYMENT_METHODS_DATA))

        val cardBrandChoice = parseCardBrandChoice(json)
        val googlePayPreference = json.optString(FIELD_GOOGLE_PAY_PREFERENCE)

        val merchantCountry = json.optString(FIELD_MERCHANT_COUNTRY)

        val passiveCaptcha = json.optJSONObject(FIELD_PASSIVE_CAPTCHA)?.let {
            PassiveCaptchaJsonParser().parse(it)
        }

        return if (stripeIntent != null) {
            ElementsSession(
                linkSettings = parseLinkSettings(linkSettings, linkFundingSources),
                paymentMethodSpecs = paymentMethodSpecs,
                stripeIntent = stripeIntent,
                customer = customer,
                merchantCountry = merchantCountry,
                cardBrandChoice = cardBrandChoice,
                isGooglePayEnabled = googlePayPreference != "disabled",
                externalPaymentMethodData = externalPaymentMethodData,
                customPaymentMethods = customPaymentMethods,
                flags = flags,
                experimentsData = experimentsData,
                orderedPaymentMethodTypesAndWallets = orderedPaymentMethodTypesAndWallets,
                elementsSessionId = elementsSessionId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                passiveCaptcha = passiveCaptcha
            )
        } else {
            null
        }
    }

    private fun parseStripeIntent(
        elementsSessionId: String?,
        paymentMethodPreference: JSONObject?,
        orderedPaymentMethodTypes: JSONArray?,
        unactivatedPaymentMethodTypes: JSONArray?,
        linkFundingSources: JSONArray?,
        countryCode: String
    ): StripeIntent? {
        return (paymentMethodPreference?.optJSONObject(params.type) ?: JSONObject()).let { json ->
            orderedPaymentMethodTypes?.let {
                json.put(
                    FIELD_PAYMENT_METHOD_TYPES,
                    orderedPaymentMethodTypes
                )
            }
            json.put(
                FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES,
                unactivatedPaymentMethodTypes
            )
            json.put(
                FIELD_LINK_FUNDING_SOURCES,
                linkFundingSources
            )
            json.put(
                FIELD_COUNTRY_CODE,
                countryCode
            )

            when (params) {
                is ElementsSessionParams.PaymentIntentType -> {
                    PaymentIntentJsonParser().parse(json)
                }
                is ElementsSessionParams.SetupIntentType -> {
                    SetupIntentJsonParser().parse(json)
                }
                is ElementsSessionParams.DeferredIntentType -> {
                    when (params.deferredIntentParams.mode) {
                        is DeferredIntentParams.Mode.Payment -> {
                            DeferredPaymentIntentJsonParser(
                                elementsSessionId = elementsSessionId,
                                paymentMode = params.deferredIntentParams.mode,
                                isLiveMode = isLiveMode,
                                timeProvider = timeProvider
                            ).parse(json)
                        }
                        is DeferredIntentParams.Mode.Setup -> {
                            DeferredSetupIntentJsonParser(
                                elementsSessionId = elementsSessionId,
                                setupMode = params.deferredIntentParams.mode,
                                isLiveMode = isLiveMode,
                                timeProvider = timeProvider
                            ).parse(json)
                        }
                    }
                }
            }
        }
    }

    private fun parseLinkSettings(
        json: JSONObject?,
        linkFundingSources: JSONArray?,
    ): ElementsSession.LinkSettings {
        val disableLinkSignup = json?.optBoolean(FIELD_DISABLE_LINK_SIGNUP) == true
        val linkPassthroughModeEnabled = json?.optBoolean(FIELD_LINK_PASSTHROUGH_MODE_ENABLED) == true
        val useLinkAttestationEndpoints = json?.optBoolean(FIELD_USE_LINK_ATTESTATION_ENDPOINTS) == true
        val disableLinkRuxInFlowController = json?.optBoolean(FIELD_LINK_MOBILE_DISABLE_RUX_IN_FLOW_CONTROLLER) == true
        val suppressLink2faModal = json?.optBoolean(FIELD_LINK_SUPPRESS_2FA_MODAL) == true
        val linkEnableDisplayableDefaultValuesInEce = json?.optBoolean(
            FIELD_LINK_ENABLE_DISPLAYABLE_DEFAULT_VALUES_IN_ECE
        ) == true

        val linkSignUpOptInFeatureEnabled = json?.optBoolean(FIELD_LINK_SIGN_UP_OPT_IN_FEATURE_ENABLED) == true
        val linkSignUpOptInInitialValue = json?.optBoolean(FIELD_LINK_SIGN_UP_OPT_IN_INITIAL_VALUE) == true

        val linkMode = json?.optString(FIELD_LINK_MODE)?.let { mode ->
            LinkMode.entries.firstOrNull { it.value == mode }
        }

        val linkFlags = json?.let { linkSettingsJson ->
            parseLinkFlags(linkSettingsJson)
        } ?: emptyMap()

        val linkConsumerIncentive = if (FeatureFlags.instantDebitsIncentives.isEnabled) {
            val linkConsumerIncentiveJson = json?.optJSONObject("link_consumer_incentive")
            linkConsumerIncentiveJson?.let { LinkConsumerIncentiveJsonParser.parse(it) }
        } else {
            null
        }

        return ElementsSession.LinkSettings(
            linkFundingSources = jsonArrayToList(linkFundingSources),
            linkPassthroughModeEnabled = linkPassthroughModeEnabled,
            linkMode = linkMode,
            linkFlags = linkFlags,
            disableLinkSignup = disableLinkSignup,
            linkConsumerIncentive = linkConsumerIncentive,
            useAttestationEndpoints = useLinkAttestationEndpoints,
            suppress2faModal = suppressLink2faModal,
            disableLinkRuxInFlowController = disableLinkRuxInFlowController,
            linkEnableDisplayableDefaultValuesInEce = linkEnableDisplayableDefaultValuesInEce,
            linkSignUpOptInFeatureEnabled = linkSignUpOptInFeatureEnabled,
            linkSignUpOptInInitialValue = linkSignUpOptInInitialValue
        )
    }

    private fun parseCustomPaymentMethods(json: JSONArray?): List<ElementsSession.CustomPaymentMethod> {
        if (json == null) {
            return emptyList()
        }

        val customPaymentMethods = (0 until json.length()).mapNotNull { index ->
            CUSTOM_PAYMENT_METHOD_JSON_PARSER.parse(json.optJSONObject(index))
        }

        return customPaymentMethods
    }

    private fun parseCustomer(
        json: JSONObject?,
        enableLinkInSpm: Boolean,
    ): ElementsSession.Customer? {
        if (json == null) {
            return null
        }

        val paymentMethods = if (enableLinkInSpm) {
            parsePaymentMethodsWithLinkDetails(json)
        } else {
            parsePaymentMethods(json)
        }

        val customerSession = parseCustomerSession(json.optJSONObject(FIELD_CUSTOMER_SESSION))
            ?: return null

        val defaultPaymentMethod = json.optString(FIELD_DEFAULT_PAYMENT_METHOD).takeIf {
            it.isNotBlank()
        }

        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            session = customerSession,
            defaultPaymentMethod = defaultPaymentMethod
        )
    }

    private fun parsePaymentMethodsWithLinkDetails(json: JSONObject): List<PaymentMethod> {
        val paymentMethodsJson = json.optJSONArray(FIELD_CUSTOMER_PAYMENT_METHODS_WITH_LINK_DETAILS)
        return paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PaymentMethodWithLinkDetailsJsonParser.parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()
    }

    private fun parsePaymentMethods(json: JSONObject): List<PaymentMethod> {
        val paymentMethodsJson = json.optJSONArray(FIELD_CUSTOMER_PAYMENT_METHODS)
        return paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PaymentMethodJsonParser().parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()
    }

    private fun parseCustomerSession(json: JSONObject?): ElementsSession.Customer.Session? {
        if (json == null) {
            return null
        }

        val id = json.optString(FIELD_CUSTOMER_ID) ?: return null
        val liveMode = json.optBoolean(FIELD_CUSTOMER_LIVE_MODE)
        val apiKey = json.optString(FIELD_CUSTOMER_API_KEY) ?: return null
        val apiKeyExpiry = json.optInt(FIELD_CUSTOMER_API_KEY_EXPIRY)
        val name = json.optString(FIELD_CUSTOMER_NAME) ?: return null
        val components = parseComponents(json.optJSONObject(FIELD_COMPONENTS)) ?: return null

        return ElementsSession.Customer.Session(
            id = id,
            liveMode = liveMode,
            apiKey = apiKey,
            apiKeyExpiry = apiKeyExpiry,
            customerId = name,
            components = components,
        )
    }

    private fun parseComponents(json: JSONObject?): ElementsSession.Customer.Components? {
        if (json == null) {
            return null
        }

        val paymentElementComponent = parsePaymentElementComponent(json.optJSONObject(FIELD_MOBILE_PAYMENT_ELEMENT))
        val customerSheetComponent = parseCustomerSheetComponent(json.optJSONObject(FIELD_CUSTOMER_SHEET))
            ?: return null

        return ElementsSession.Customer.Components(
            mobilePaymentElement = paymentElementComponent,
            customerSheet = customerSheetComponent
        )
    }

    private fun parsePaymentElementComponent(
        json: JSONObject?
    ): ElementsSession.Customer.Components.MobilePaymentElement {
        if (json == null) {
            return ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        }

        val paymentSheetEnabled = json.optBoolean(FIELD_ENABLED)

        return if (paymentSheetEnabled) {
            val paymentSheetFeatures = json.optJSONObject(FIELD_FEATURES)
                ?: return ElementsSession.Customer.Components.MobilePaymentElement.Disabled

            val paymentMethodSaveFeature = paymentSheetFeatures.optString(FIELD_PAYMENT_METHOD_SAVE)
            val paymentMethodRemoveFeature = paymentSheetFeatures.optString(FIELD_PAYMENT_METHOD_REMOVE)
            val paymentMethodRemoveLastFeature = paymentSheetFeatures.optString(FIELD_PAYMENT_METHOD_REMOVE_LAST)
            val paymentMethodSetAsDefaultFeature = paymentSheetFeatures.optString(FIELD_PAYMENT_METHOD_SET_AS_DEFAULT)
            val allowRedisplayOverrideValue = paymentSheetFeatures
                .optString(FIELD_PAYMENT_METHOD_ALLOW_REDISPLAY_OVERRIDE)

            val allowRedisplayOverride = PaymentMethod.AllowRedisplay.entries.firstOrNull { allowRedisplay ->
                allowRedisplay.value == allowRedisplayOverrideValue
            }

            ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                isPaymentMethodSaveEnabled = paymentMethodSaveFeature == VALUE_ENABLED,
                isPaymentMethodRemoveEnabled = paymentMethodRemoveFeature == VALUE_ENABLED,
                canRemoveLastPaymentMethod = paymentMethodRemoveLastFeature == VALUE_ENABLED,
                isPaymentMethodSetAsDefaultEnabled = paymentMethodSetAsDefaultFeature == VALUE_ENABLED,
                allowRedisplayOverride = allowRedisplayOverride,
            )
        } else {
            ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        }
    }

    private fun parseCustomerSheetComponent(json: JSONObject?): ElementsSession.Customer.Components.CustomerSheet? {
        if (json == null) {
            return ElementsSession.Customer.Components.CustomerSheet.Disabled
        }

        val customerSheetEnabled = json.optBoolean(FIELD_ENABLED)

        return if (customerSheetEnabled) {
            val customerSheetFeatures = json.optJSONObject(FIELD_FEATURES)
                ?: return ElementsSession.Customer.Components.CustomerSheet.Disabled

            val paymentMethodRemoveFeature = customerSheetFeatures.optString(FIELD_PAYMENT_METHOD_REMOVE)
            val paymentMethodRemoveLastFeature = customerSheetFeatures.optString(FIELD_PAYMENT_METHOD_REMOVE_LAST)
            val paymentMethodSyncDefaultFeature = customerSheetFeatures.optString(FIELD_PAYMENT_METHOD_SYNC_DEFAULT)

            ElementsSession.Customer.Components.CustomerSheet.Enabled(
                isPaymentMethodRemoveEnabled = paymentMethodRemoveFeature == VALUE_ENABLED,
                canRemoveLastPaymentMethod = paymentMethodRemoveLastFeature == VALUE_ENABLED,
                isPaymentMethodSyncDefaultEnabled = paymentMethodSyncDefaultFeature == VALUE_ENABLED,
            )
        } else {
            ElementsSession.Customer.Components.CustomerSheet.Disabled
        }
    }

    private fun parseCardBrandChoice(json: JSONObject): ElementsSession.CardBrandChoice? {
        val cardBrandChoice = json.optJSONObject(FIELD_CARD_BRAND_CHOICE) ?: return null
        val preferredNetworks = mutableListOf<String>()

        cardBrandChoice.optJSONArray(FIELD_PREFERRED_NETWORKS)?.let { jsonArray ->
            for (index in 0 until jsonArray.length()) {
                jsonArray.optString(index)?.let {
                    preferredNetworks.add(it)
                }
            }
        }

        return ElementsSession.CardBrandChoice(
            eligible = cardBrandChoice.optBoolean(FIELD_ELIGIBLE, false),
            preferredNetworks = preferredNetworks.toList()
        )
    }

    private fun parseLinkFlags(json: JSONObject): Map<String, Boolean> {
        val flags = mutableMapOf<String, Boolean>()

        json.keys().forEach { key ->
            val value = json.get(key)

            if (value is Boolean) {
                flags[key] = value
            }
        }

        return flags.toMap()
    }

    /**
     * Parse the flags from the [json] object.
     * @param json the json object to parse
     */
    private fun parseSessionFlags(json: JSONObject): Map<ElementsSession.Flag, Boolean> {
        val flags = mutableMapOf<ElementsSession.Flag, Boolean>()

        json.keys().forEach { key ->
            val value = json.get(key)
            ElementsSession.Flag.entries
                .firstOrNull { it.flagValue == key }
                ?.let { flag ->
                    if (value is Boolean) {
                        flags[flag] = value
                    }
                }
        }

        return flags.toMap()
    }

    /**
     * Parse the experiment assignments from the [json] object.
     * @param json the json object to parse
     */
    private fun parseExperimentAssignments(json: JSONObject): Map<ExperimentAssignment, String> {
        val specs = mutableMapOf<ExperimentAssignment, String>()
        json.keys().forEach { key ->
            val value = json.get(key)
            ExperimentAssignment.entries
                .firstOrNull { it.experimentValue == key }
                ?.let { flag ->
                    if (value is String) {
                        specs[flag] = value
                    }
                }
        }

        return specs.toMap()
    }

    internal companion object {
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ELEMENTS_SESSION_ID = "session_id"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPESAND_WALLETS = "ordered_payment_method_types_and_wallets"
        private const val FIELD_LINK_SETTINGS = "link_settings"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
        private const val FIELD_FLAGS = "flags"
        private const val FIELD_LINK_PASSTHROUGH_MODE_ENABLED = "link_passthrough_mode_enabled"
        private const val FIELD_LINK_MODE = "link_mode"
        private const val FIELD_DISABLE_LINK_SIGNUP = "link_mobile_disable_signup"
        private const val FIELD_USE_LINK_ATTESTATION_ENDPOINTS = "link_mobile_use_attestation_endpoints"
        private const val FIELD_LINK_MOBILE_DISABLE_RUX_IN_FLOW_CONTROLLER =
            "link_mobile_disable_rux_in_flow_controller"
        private const val FIELD_LINK_SUPPRESS_2FA_MODAL = "link_mobile_suppress_2fa_modal"
        private const val FIELD_LINK_ENABLE_DISPLAYABLE_DEFAULT_VALUES_IN_ECE =
            "link_enable_displayable_default_values_in_ece"
        private const val FIELD_LINK_SIGN_UP_OPT_IN_FEATURE_ENABLED = "link_sign_up_opt_in_feature_enabled"
        private const val FIELD_LINK_SIGN_UP_OPT_IN_INITIAL_VALUE = "link_sign_up_opt_in_initial_value"
        private const val FIELD_MERCHANT_COUNTRY = "merchant_country"
        private const val FIELD_PAYMENT_METHOD_PREFERENCE = "payment_method_preference"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_SPECS = "payment_method_specs"
        private const val FIELD_CARD_BRAND_CHOICE = "card_brand_choice"
        private const val FIELD_ELIGIBLE = "eligible"
        private const val FIELD_PREFERRED_NETWORKS = "preferred_networks"
        private const val FIELD_EXTERNAL_PAYMENT_METHOD_DATA = "external_payment_method_data"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_CUSTOMER_PAYMENT_METHODS = "payment_methods"
        private const val FIELD_CUSTOMER_PAYMENT_METHODS_WITH_LINK_DETAILS = "payment_methods_with_link_details"
        private const val FIELD_CUSTOMER_SESSION = "customer_session"
        private const val FIELD_DEFAULT_PAYMENT_METHOD = "default_payment_method"
        private const val FIELD_CUSTOM_PAYMENT_METHODS_DATA = "custom_payment_method_data"
        private const val FIELD_CUSTOMER_ID = "id"
        private const val FIELD_CUSTOMER_LIVE_MODE = "livemode"
        private const val FIELD_CUSTOMER_API_KEY = "api_key"
        private const val FIELD_CUSTOMER_API_KEY_EXPIRY = "api_key_expiry"
        private const val FIELD_CUSTOMER_NAME = "customer"
        private const val FIELD_COMPONENTS = "components"
        private const val FIELD_MOBILE_PAYMENT_ELEMENT = "mobile_payment_element"
        private const val FIELD_CUSTOMER_SHEET = "customer_sheet"
        private const val FIELD_ENABLED = "enabled"
        private const val FIELD_FEATURES = "features"
        private const val FIELD_PAYMENT_METHOD_SAVE = "payment_method_save"
        private const val FIELD_PAYMENT_METHOD_REMOVE = "payment_method_remove"
        private const val FIELD_PAYMENT_METHOD_ALLOW_REDISPLAY_OVERRIDE =
            "payment_method_save_allow_redisplay_override"
        private const val FIELD_PAYMENT_METHOD_REMOVE_LAST = "payment_method_remove_last"
        private const val FIELD_PAYMENT_METHOD_SET_AS_DEFAULT = "payment_method_set_as_default"
        private const val FIELD_PAYMENT_METHOD_SYNC_DEFAULT = "payment_method_sync_default"
        private const val VALUE_ENABLED = FIELD_ENABLED
        const val FIELD_GOOGLE_PAY_PREFERENCE = "google_pay_preference"
        private const val FIELD_EXPERIMENTS_DATA = "experiments_data"
        private const val FIELD_EXPERIMENTS_ASSIGNMENTS = "experiment_assignments"
        private const val FIELD_PASSIVE_CAPTCHA = "passive_captcha"
        private const val ARB_ID = "arb_id"

        private val CUSTOM_PAYMENT_METHOD_JSON_PARSER = CustomPaymentMethodJsonParser()
    }
}
