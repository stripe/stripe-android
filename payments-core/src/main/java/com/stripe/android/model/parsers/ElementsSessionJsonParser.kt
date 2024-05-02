package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import org.json.JSONArray
import org.json.JSONObject

internal class ElementsSessionJsonParser(
    private val params: ElementsSessionParams,
    private val apiKey: String,
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
        val unactivatedPaymentMethodTypes =
            jsonArrayToList(json.optJSONArray(FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES))
                .map { it.lowercase() }
        val paymentMethodSpecs = json.optJSONArray(FIELD_PAYMENT_METHOD_SPECS)?.toString()
        val externalPaymentMethodData = json.optJSONArray(FIELD_EXTERNAL_PAYMENT_METHOD_DATA)?.toString()
        val linkFundingSources = json.optJSONObject(FIELD_LINK_SETTINGS)?.optJSONArray(
            FIELD_LINK_FUNDING_SOURCES
        )
        val linkPassthroughModeEnabled = json.optJSONObject(FIELD_LINK_SETTINGS)
            ?.optBoolean(FIELD_LINK_PASSTHROUGH_MODE_ENABLED) ?: false
        val disableLinkSignup = json.optJSONObject(FIELD_LINK_SETTINGS)
            ?.optBoolean(FIELD_DISABLE_LINK_SIGNUP) ?: false
        val linkFlags = json.optJSONObject(FIELD_LINK_SETTINGS)?.let { linkSettingsJson ->
            parseLinkFlags(linkSettingsJson)
        } ?: emptyMap()
        val orderedPaymentMethodTypes =
            paymentMethodPreference.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES)

        val elementsSessionId = json.optString(FIELD_ELEMENTS_SESSION_ID)
        val customer = parseCustomer(json.optJSONObject(FIELD_CUSTOMER))

        val stripeIntent = parseStripeIntent(
            elementsSessionId = elementsSessionId,
            paymentMethodPreference = paymentMethodPreference,
            orderedPaymentMethodTypes = orderedPaymentMethodTypes,
            unactivatedPaymentMethodTypes = unactivatedPaymentMethodTypes,
            linkFundingSources = linkFundingSources,
            countryCode = countryCode
        )

        val merchantCountry = json.optString(FIELD_MERCHANT_COUNTRY)

        val isEligibleForCardBrandChoice = parseCardBrandChoiceEligibility(json)
        val googlePayPreference = json.optString(FIELD_GOOGLE_PAY_PREFERENCE)

        return if (stripeIntent != null) {
            ElementsSession(
                linkSettings = ElementsSession.LinkSettings(
                    linkFundingSources = jsonArrayToList(linkFundingSources),
                    linkPassthroughModeEnabled = linkPassthroughModeEnabled,
                    linkFlags = linkFlags,
                    disableLinkSignup = disableLinkSignup,
                ),
                paymentMethodSpecs = paymentMethodSpecs,
                stripeIntent = stripeIntent,
                customer = customer,
                merchantCountry = merchantCountry,
                isEligibleForCardBrandChoice = isEligibleForCardBrandChoice,
                isGooglePayEnabled = googlePayPreference != "disabled",
                externalPaymentMethodData = externalPaymentMethodData,
            )
        } else {
            null
        }
    }

    private fun parseStripeIntent(
        elementsSessionId: String?,
        paymentMethodPreference: JSONObject?,
        orderedPaymentMethodTypes: JSONArray?,
        unactivatedPaymentMethodTypes: List<String>,
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
                                apiKey = apiKey,
                                timeProvider = timeProvider
                            ).parse(json)
                        }
                        is DeferredIntentParams.Mode.Setup -> {
                            DeferredSetupIntentJsonParser(
                                elementsSessionId = elementsSessionId,
                                setupMode = params.deferredIntentParams.mode,
                                apiKey = apiKey,
                                timeProvider = timeProvider
                            ).parse(json)
                        }
                    }
                }
            }
        }
    }

    private fun parseCustomer(json: JSONObject?): ElementsSession.Customer? {
        if (json == null) {
            return null
        }

        val paymentMethodsJson = json.optJSONArray(FIELD_CUSTOMER_PAYMENT_METHODS)
        val paymentMethods = paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PAYMENT_METHOD_JSON_PARSER.parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()

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

    private fun parseCustomerSession(json: JSONObject?): ElementsSession.Customer.Session? {
        if (json == null) {
            return null
        }

        val id = json.optString(FIELD_CUSTOMER_ID) ?: return null
        val liveMode = json.optBoolean(FIELD_CUSTOMER_LIVE_MODE)
        val apiKey = json.optString(FIELD_CUSTOMER_API_KEY) ?: return null
        val apiKeyExpiry = json.optInt(FIELD_CUSTOMER_API_KEY_EXPIRY)
        val name = json.optString(FIELD_CUSTOMER_NAME) ?: return null

        return ElementsSession.Customer.Session(
            id = id,
            liveMode = liveMode,
            apiKey = apiKey,
            apiKeyExpiry = apiKeyExpiry,
            customerId = name
        )
    }

    private fun parseCardBrandChoiceEligibility(json: JSONObject): Boolean {
        val cardBrandChoice = json.optJSONObject(FIELD_CARD_BRAND_CHOICE) ?: return false
        return cardBrandChoice.optBoolean(FIELD_ELIGIBLE, false)
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

    internal companion object {
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ELEMENTS_SESSION_ID = "session_id"
        private const val FIELD_COUNTRY_CODE = "country_code"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES = "ordered_payment_method_types"
        private const val FIELD_LINK_SETTINGS = "link_settings"
        private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
        private const val FIELD_LINK_PASSTHROUGH_MODE_ENABLED = "link_passthrough_mode_enabled"
        private const val FIELD_DISABLE_LINK_SIGNUP = "link_mobile_disable_signup"
        private const val FIELD_MERCHANT_COUNTRY = "merchant_country"
        private const val FIELD_PAYMENT_METHOD_PREFERENCE = "payment_method_preference"
        private const val FIELD_UNACTIVATED_PAYMENT_METHOD_TYPES = "unactivated_payment_method_types"
        private const val FIELD_PAYMENT_METHOD_SPECS = "payment_method_specs"
        private const val FIELD_CARD_BRAND_CHOICE = "card_brand_choice"
        private const val FIELD_ELIGIBLE = "eligible"
        private const val FIELD_EXTERNAL_PAYMENT_METHOD_DATA = "external_payment_method_data"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_CUSTOMER_PAYMENT_METHODS = "payment_methods"
        private const val FIELD_CUSTOMER_SESSION = "customer_session"
        private const val FIELD_DEFAULT_PAYMENT_METHOD = "default_payment_method"
        private const val FIELD_CUSTOMER_ID = "id"
        private const val FIELD_CUSTOMER_LIVE_MODE = "livemode"
        private const val FIELD_CUSTOMER_API_KEY = "api_key"
        private const val FIELD_CUSTOMER_API_KEY_EXPIRY = "api_key_expiry"
        private const val FIELD_CUSTOMER_NAME = "customer"
        const val FIELD_GOOGLE_PAY_PREFERENCE = "google_pay_preference"

        private val PAYMENT_METHOD_JSON_PARSER = PaymentMethodJsonParser()
    }
}
