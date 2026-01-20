package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import org.json.JSONObject

/**
 * Parser for the checkout session init API response (`/v1/payment_pages/{cs_id}/init`).
 *
 * The response contains both checkout session metadata (`id`, `amount`, `currency`) and an
 * embedded `elements_session` object. This parser extracts the checkout fields first, then
 * delegates `elements_session` parsing to [ElementsSessionJsonParser].
 */
internal class CheckoutSessionResponseJsonParser(
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        // 1. Extract checkout session fields
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = json.optLong(FIELD_AMOUNT, -1).takeIf { it >= 0 } ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null

        // 2. Create ElementsSessionParams using extracted amount/currency
        // Since payment_method_preference.type is "deferred_intent", we use DeferredIntentType
        val elementsSessionParams = ElementsSessionParams.DeferredIntentType(
            deferredIntentParams = DeferredIntentParams(
                mode = DeferredIntentParams.Mode.Payment(
                    amount = amount,
                    currency = currency,
                    captureMethod = PaymentIntent.CaptureMethod.Automatic,
                    setupFutureUsage = null,
                    paymentMethodOptionsJsonString = null,
                ),
                paymentMethodTypes = emptyList(), // Populated from elements_session
                paymentMethodConfigurationId = null,
                onBehalfOf = null,
            ),
            customPaymentMethods = emptyList(),
            externalPaymentMethods = emptyList(),
            appId = "", // Not needed for parsing, only for API requests
        )

        // 3. Parse elements_session with the constructed params
        val elementsSessionJson = json.optJSONObject(FIELD_ELEMENTS_SESSION) ?: return null
        val elementsSession = ElementsSessionJsonParser(
            params = elementsSessionParams,
            isLiveMode = isLiveMode,
        ).parse(elementsSessionJson) ?: return null

        return CheckoutSessionResponse(
            id = id,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_ELEMENTS_SESSION = "elements_session"
    }
}
