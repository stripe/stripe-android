package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

internal object CheckoutClientResponseJsonParser : ModelJsonParser<CheckoutSessionResponse> {
    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        val sessionId = StripeJsonUtils.optString(json, FIELD_SESSION_ID)
            ?: StripeJsonUtils.optString(json, FIELD_ID)
            ?: return null
        val currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY) ?: return null
        val totalSummaryJson = json.optJSONObject(FIELD_TOTAL_SUMMARY) ?: return null
        val amount = totalSummaryJson.optLong(FIELD_DUE, -1).takeIf { it >= 0 } ?: return null
        val liveMode = json.optBoolean(FIELD_LIVE_MODE, false)
        val countryCode = StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY_CODE)
        val paymentResponse = json.optJSONObject(FIELD_PAYMENT_RESPONSE)

        return CheckoutSessionResponse(
            id = sessionId,
            amount = amount,
            currency = currency,
            mode = CheckoutSessionResponse.Mode.PAYMENT,
            status = parseStatus(json.optString(FIELD_STATE)),
            liveMode = liveMode,
            taxStatus = CheckoutSessionResponse.TaxStatus.UNKNOWN,
            customerEmail = null,
            elementsSession = parseElementsSession(
                json = json,
                sessionId = sessionId,
                amount = amount,
                currency = currency,
                liveMode = liveMode,
                countryCode = countryCode,
                paymentResponse = paymentResponse,
            ),
            paymentIntent = null,
            setupIntent = null,
            customer = null,
            savedPaymentMethodsOfferSave = null,
            totalSummary = parseTotalSummary(totalSummaryJson),
            lineItems = parseLineItems(json),
            shippingOptions = emptyList(),
            adaptivePricingInfo = null,
            automaticTaxEnabled = false,
            taxAddressSource = null,
            allowedShippingCountries = null,
            requiresBillingAddress = false,
            merchantCountry = countryCode,
        )
    }

    private fun parseStatus(state: String): CheckoutSessionResponse.Status {
        return when (state) {
            "complete" -> CheckoutSessionResponse.Status.COMPLETE
            "expired" -> CheckoutSessionResponse.Status.EXPIRED
            "ready", "processing", "requires_action" -> CheckoutSessionResponse.Status.OPEN
            else -> CheckoutSessionResponse.Status.UNKNOWN
        }
    }

    private fun parseTotalSummary(json: JSONObject): CheckoutSessionResponse.TotalSummaryResponse {
        val due = json.optLong(FIELD_DUE, 0)
        return CheckoutSessionResponse.TotalSummaryResponse(
            subtotal = json.optLong(FIELD_SUBTOTAL, due),
            totalDueToday = due,
            totalAmountDue = json.optLong(FIELD_TOTAL, due),
            discountAmounts = emptyList(),
            taxAmounts = emptyList(),
            shippingRate = null,
            appliedBalance = null,
        )
    }

    private fun parseLineItems(json: JSONObject): List<CheckoutSessionResponse.LineItem> {
        val items = json.optJSONArray(FIELD_ITEMS) ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val id = StripeJsonUtils.optString(item, FIELD_KEY) ?: return@mapNotNull null
            val priceItem = item.optJSONObject(FIELD_ONE_TIME_PRICE)
                ?.optJSONArray(FIELD_ITEMS)
                ?.optJSONObject(0)
            val price = priceItem?.optJSONObject(FIELD_PRICE)
            val quantity = priceItem?.optInt(FIELD_QUANTITY, -1)?.takeIf { it > 0 } ?: 1
            val unitAmount = parseUnitAmount(price)
            val subtotal = unitAmount?.times(quantity) ?: 0L

            CheckoutSessionResponse.LineItem(
                id = id,
                name = parsePriceName(price) ?: id,
                quantity = quantity,
                unitAmount = unitAmount,
                subtotal = subtotal,
                total = subtotal,
            )
        }
    }

    private fun parseElementsSession(
        json: JSONObject,
        sessionId: String,
        amount: Long,
        currency: String,
        liveMode: Boolean,
        countryCode: String?,
        paymentResponse: JSONObject?,
    ): ElementsSession {
        val orderedPaymentMethodTypesAndWallets = jsonArrayToList(
            json.optJSONArray(FIELD_ORDERED_PAYMENT_METHOD_TYPES_AND_WALLETS)
        )
        val paymentMethodTypes = parsePaymentMethodTypes(json, orderedPaymentMethodTypesAndWallets)
        val linkSettingsJson = json.optJSONObject(FIELD_LINK_SETTINGS)
        val linkFundingSources = jsonArrayToList(
            linkSettingsJson?.optJSONArray(FIELD_LINK_FUNDING_SOURCES)
        )
        val paymentMethodOptionsJsonString = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS)?.toString()

        return ElementsSession(
            linkSettings = parseLinkSettings(linkSettingsJson, linkFundingSources),
            paymentMethodSpecs = json.optJSONArray(FIELD_PAYMENT_METHOD_SPECS)?.toString(),
            externalPaymentMethodData = null,
            stripeIntent = PaymentIntent(
                id = sessionId,
                paymentMethodTypes = paymentMethodTypes,
                amount = amount,
                captureMethod = PaymentIntent.CaptureMethod.Automatic,
                clientSecret = null,
                countryCode = countryCode,
                created = 0L,
                currency = currency,
                isLiveMode = liveMode,
                paymentMethodId = StripeJsonUtils.optString(paymentResponse, FIELD_PAYMENT_METHOD),
                status = parseIntentStatus(paymentResponse),
                unactivatedPaymentMethods = emptyList(),
                linkFundingSources = linkFundingSources.map { it.lowercase() },
                paymentMethodOptionsJsonString = paymentMethodOptionsJsonString,
                automaticPaymentMethodsEnabled = paymentMethodTypes.isEmpty(),
            ),
            orderedPaymentMethodTypesAndWallets = orderedPaymentMethodTypesAndWallets,
            flags = emptyMap(),
            experimentsData = null,
            customer = null,
            merchantCountry = countryCode,
            merchantLogoUrl = null,
            cardBrandChoice = null,
            isGooglePayEnabled = GOOGLE_PAY in orderedPaymentMethodTypesAndWallets,
            customPaymentMethods = emptyList(),
            elementsSessionId = sessionId,
            passiveCaptcha = null,
            elementsSessionConfigId = null,
            accountId = null,
            merchantId = null,
        )
    }

    private fun parsePaymentMethodTypes(
        json: JSONObject,
        orderedPaymentMethodTypesAndWallets: List<String>,
    ): List<String> {
        val specs = json.optJSONArray(FIELD_PAYMENT_METHOD_SPECS)
        val typesFromSpecs = specs?.let {
            (0 until it.length()).mapNotNull { index ->
                StripeJsonUtils.optString(it.optJSONObject(index), FIELD_TYPE)
            }
        }.orEmpty()

        return typesFromSpecs.ifEmpty {
            orderedPaymentMethodTypesAndWallets.filterNot { it in WALLET_CODES }
        }
    }

    private fun parseLinkSettings(
        json: JSONObject?,
        linkFundingSources: List<String>,
    ): ElementsSession.LinkSettings {
        val linkMode = json?.optString(FIELD_LINK_MODE)?.let { mode ->
            LinkMode.entries.firstOrNull { it.value == mode }
        }
        val linkBrand = if (FeatureFlags.forceOnelink.isEnabled) {
            LinkBrand.Onelink
        } else {
            json?.optString(FIELD_LINK_BRAND)
                ?.takeIf { it.isNotEmpty() }
                ?.let { brand -> LinkBrand.entries.firstOrNull { it.value == brand } }
                ?: LinkBrand.Link
        }

        return ElementsSession.LinkSettings(
            linkFundingSources = linkFundingSources,
            linkPassthroughModeEnabled = json?.optBoolean(FIELD_LINK_PASSTHROUGH_MODE_ENABLED) == true,
            linkMode = linkMode,
            linkFlags = parseBooleanMap(json),
            disableLinkSignup = json?.optBoolean(FIELD_DISABLE_LINK_SIGNUP) == true,
            linkConsumerIncentive = null,
            useAttestationEndpoints = json?.optBoolean(FIELD_USE_LINK_ATTESTATION_ENDPOINTS) == true,
            suppress2faModal = json?.optBoolean(FIELD_LINK_SUPPRESS_2FA_MODAL) == true,
            disableLinkRuxInFlowController = json?.optBoolean(FIELD_LINK_MOBILE_DISABLE_RUX_IN_FLOW_CONTROLLER) == true,
            linkEnableDisplayableDefaultValuesInEce =
                json?.optBoolean(FIELD_LINK_ENABLE_DISPLAYABLE_DEFAULT_VALUES_IN_ECE) == true,
            linkSignUpOptInFeatureEnabled = json?.optBoolean(FIELD_LINK_SIGN_UP_OPT_IN_FEATURE_ENABLED) == true,
            linkSignUpOptInInitialValue = json?.optBoolean(FIELD_LINK_SIGN_UP_OPT_IN_INITIAL_VALUE) == true,
            linkSupportedPaymentMethodsOnboardingEnabled = jsonArrayToList(
                json?.optJSONArray(FIELD_LINK_SUPPORTED_PAYMENT_METHODS_ONBOARDING_ENABLED)
            ),
            linkBrand = linkBrand,
        )
    }

    private fun parseBooleanMap(json: JSONObject?): Map<String, Boolean> {
        if (json == null) {
            return emptyMap()
        }
        val result = mutableMapOf<String, Boolean>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.opt(key)
            if (value is Boolean) {
                result[key] = value
            }
        }
        return result
    }

    private fun parseIntentStatus(paymentResponse: JSONObject?): StripeIntent.Status? {
        val status = StripeJsonUtils.optString(paymentResponse, FIELD_STATUS)
        return StripeIntent.Status.entries.firstOrNull { it.code == status }
    }

    private fun parsePriceName(price: JSONObject?): String? {
        return price?.optJSONObject(FIELD_PRODUCT)?.let { product ->
            StripeJsonUtils.optString(product, FIELD_NAME)
        } ?: StripeJsonUtils.optString(price, FIELD_NICKNAME)
    }

    private fun parseUnitAmount(price: JSONObject?): Long? {
        return price?.optLong(FIELD_UNIT_AMOUNT, -1)?.takeIf { it >= 0 }
            ?: StripeJsonUtils.optString(price, FIELD_UNIT_AMOUNT_DECIMAL)
                ?.substringBefore(".")
                ?.toLongOrNull()
    }

    private const val FIELD_COUNTRY_CODE = "country_code"
    private const val FIELD_CURRENCY = "currency"
    private const val FIELD_DUE = "due"
    private const val FIELD_ID = "id"
    private const val FIELD_ITEMS = "items"
    private const val FIELD_KEY = "key"
    private const val FIELD_DISABLE_LINK_SIGNUP = "link_mobile_disable_signup"
    private const val FIELD_LINK_BRAND = "link_brand"
    private const val FIELD_LINK_ENABLE_DISPLAYABLE_DEFAULT_VALUES_IN_ECE =
        "link_enable_displayable_default_values_in_ece"
    private const val FIELD_LINK_FUNDING_SOURCES = "link_funding_sources"
    private const val FIELD_LINK_MOBILE_DISABLE_RUX_IN_FLOW_CONTROLLER =
        "link_mobile_disable_rux_in_flow_controller"
    private const val FIELD_LINK_MODE = "link_mode"
    private const val FIELD_LINK_PASSTHROUGH_MODE_ENABLED = "link_passthrough_mode_enabled"
    private const val FIELD_LINK_SETTINGS = "link_settings"
    private const val FIELD_LINK_SIGN_UP_OPT_IN_FEATURE_ENABLED = "link_sign_up_opt_in_feature_enabled"
    private const val FIELD_LINK_SIGN_UP_OPT_IN_INITIAL_VALUE = "link_sign_up_opt_in_initial_value"
    private const val FIELD_LINK_SUPPORTED_PAYMENT_METHODS_ONBOARDING_ENABLED =
        "link_supported_payment_methods_onboarding_enabled"
    private const val FIELD_LINK_SUPPRESS_2FA_MODAL = "link_mobile_suppress_2fa_modal"
    private const val FIELD_LIVE_MODE = "livemode"
    private const val FIELD_NAME = "name"
    private const val FIELD_NICKNAME = "nickname"
    private const val FIELD_ONE_TIME_PRICE = "one_time_price"
    private const val FIELD_ORDERED_PAYMENT_METHOD_TYPES_AND_WALLETS = "ordered_payment_method_types_and_wallets"
    private const val FIELD_PAYMENT_METHOD = "payment_method"
    private const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
    private const val FIELD_PAYMENT_METHOD_SPECS = "payment_method_specs"
    private const val FIELD_PAYMENT_RESPONSE = "payment_response"
    private const val FIELD_PRICE = "price"
    private const val FIELD_PRODUCT = "product"
    private const val FIELD_QUANTITY = "quantity"
    private const val FIELD_SESSION_ID = "session_id"
    private const val FIELD_STATE = "state"
    private const val FIELD_STATUS = "status"
    private const val FIELD_SUBTOTAL = "subtotal"
    private const val FIELD_TOTAL = "total"
    private const val FIELD_TOTAL_SUMMARY = "total_summary"
    private const val FIELD_TYPE = "type"
    private const val FIELD_UNIT_AMOUNT = "unit_amount"
    private const val FIELD_UNIT_AMOUNT_DECIMAL = "unit_amount_decimal"
    private const val FIELD_USE_LINK_ATTESTATION_ENDPOINTS = "link_mobile_use_attestation_endpoints"

    private const val GOOGLE_PAY = "google_pay"
    private val WALLET_CODES = setOf("apple_pay", GOOGLE_PAY)
}
