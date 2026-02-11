package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import org.json.JSONObject

/**
 * Parser for checkout session API responses:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns elements_session
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns payment_intent
 *
 * The init response contains checkout session metadata (`id`, `amount`, `currency`) and an
 * embedded `elements_session` object. The confirm response contains a `payment_intent` object.
 *
 * For confirm responses, this parser extracts the `payment_intent` and creates a minimal
 * response with the payment intent data.
 */
internal class CheckoutSessionResponseJsonParser(
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        val sessionId = json.optString(FIELD_SESSION_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = extractDueAmount(json) ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null
        val paymentIntent = json.optJSONObject(FIELD_PAYMENT_INTENT)?.let {
            PaymentIntentJsonParser().parse(it)
        }

        val elementsSession = parseElementsSession(
            json.optJSONObject(FIELD_SERVER_BUILT_ELEMENTS_SESSION_PARAMS),
            json.optJSONObject(FIELD_ELEMENTS_SESSION),
        )

        return CheckoutSessionResponse(
            id = sessionId,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
        )
    }

    private fun parseElementsSessionParams(
        serverBuiltElementsSessionParams: JSONObject,
    ): ElementsSessionParams? {
        return when (serverBuiltElementsSessionParams.optString("type")) {
            "deferred_intent" -> {
                val deferredIntentJson = serverBuiltElementsSessionParams.optJSONObject("deferred_intent")
                    ?: return null
                ElementsSessionParams.DeferredIntentType(
                    locale = serverBuiltElementsSessionParams.optString("locale"),
                    deferredIntentParams = DeferredIntentParams(
                        mode = DeferredIntentParams.parseModeFromJson(deferredIntentJson),
                        paymentMethodTypes = jsonArrayToList(
                            deferredIntentJson.optJSONArray("payment_method_types")
                        ),
                        paymentMethodConfigurationId = deferredIntentJson
                            .optString("payment_method_configuration"),
                        onBehalfOf = deferredIntentJson.optString("on_behalf_of")
                    ),
                    customPaymentMethods = jsonArrayToList(
                        serverBuiltElementsSessionParams.optJSONArray("custom_payment_methods")
                    ),
                    externalPaymentMethods = jsonArrayToList(
                        serverBuiltElementsSessionParams.optJSONArray("external_payment_methods")
                    ),
                    savedPaymentMethodSelectionId = serverBuiltElementsSessionParams
                        .optString("client_default_payment_method"),
                    mobileSessionId = serverBuiltElementsSessionParams.optString("mobile_session_id"),
                    appId = serverBuiltElementsSessionParams.optString("mobile_app_id"),
                    countryOverride = serverBuiltElementsSessionParams.optString("country_override")
                )
            }
            else -> {
                // This function is only used for parsing elements session params when init payment_pages
                // The params is always deferred intent type.
                null
            }
        }
    }

    /**
     * Parses the elements_session object if present.
     */
    private fun parseElementsSession(
        serverBuiltElementsSessionParams: JSONObject?,
        elementsSessionJson: JSONObject?,
    ): ElementsSession? {
        val serverBuiltElementsSessionParams = serverBuiltElementsSessionParams?.let {
            parseElementsSessionParams(it)
        } ?: return null
        val elementsSessionJson = elementsSessionJson ?: return null

        return ElementsSessionJsonParser(
            serverBuiltElementsSessionParams,
            isLiveMode = isLiveMode,
        ).parse(elementsSessionJson)
    }

    /**
     * Extracts amount from `total_summary.due` in response JSON.
     */
    private fun extractDueAmount(json: JSONObject): Long? {
        val totalSummary = json.optJSONObject(FIELD_TOTAL_SUMMARY) ?: return null
        val due = totalSummary.optLong(FIELD_DUE, -1)
        return if (due >= 0) due else null
    }

    private companion object {
        private const val FIELD_SESSION_ID = "session_id"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_ELEMENTS_SESSION = "elements_session"
        private const val FIELD_TOTAL_SUMMARY = "total_summary"
        private const val FIELD_DUE = "due"
        private const val FIELD_PAYMENT_INTENT = "payment_intent"
        private const val FIELD_SERVER_BUILT_ELEMENTS_SESSION_PARAMS = "server_built_elements_session_params"
    }
}
