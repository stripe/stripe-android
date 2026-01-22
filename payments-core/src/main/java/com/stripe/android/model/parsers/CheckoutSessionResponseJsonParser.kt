package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import org.json.JSONObject

/**
 * Parser for checkout session API responses:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns elements_session
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns payment_intent
 *
 * The init response contains checkout session metadata (`id`, `amount`, `currency`) and an
 * embedded `elements_session` object. The confirm response contains a `payment_intent` object.
 *
 * For init responses, this parser extracts the checkout fields first, then delegates
 * `elements_session` parsing to [ElementsSessionJsonParser].
 *
 * For confirm responses, this parser extracts the `payment_intent` and creates a minimal
 * response with the payment intent data.
 */
internal class CheckoutSessionResponseJsonParser(
    private val elementsSessionParams: ElementsSessionParams.CheckoutSessionType,
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        // Try to parse payment_intent first (confirm response)
        val paymentIntentJson = json.optJSONObject(FIELD_PAYMENT_INTENT)
        val paymentIntent = paymentIntentJson?.let {
            PaymentIntentJsonParser().parse(it)
        }

        // If payment_intent exists, this is a confirm response
        if (paymentIntent != null) {
            return parseConfirmResponse(json, paymentIntent)
        }

        // Otherwise, parse as init response
        return parseInitResponse(json)
    }

    /**
     * Parses a confirm response which contains a payment_intent.
     */
    private fun parseConfirmResponse(
        json: JSONObject,
        paymentIntent: PaymentIntent,
    ): CheckoutSessionResponse? {
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() }
            ?: paymentIntent.id?.substringBefore("_secret_")
            ?: return null

        // For confirm responses, amount/currency come from the payment intent
        val amount = paymentIntent.amount ?: 0L
        val currency = paymentIntent.currency ?: ""

        // Elements session may or may not be present in confirm response
        val elementsSession = parseElementsSession(json, amount, currency)
            ?: createMinimalElementsSession(paymentIntent)

        return CheckoutSessionResponse(
            id = id,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
        )
    }

    /**
     * Parses an init response which contains elements_session.
     */
    private fun parseInitResponse(json: JSONObject): CheckoutSessionResponse? {
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = extractAmount(json) ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null

        val elementsSession = parseElementsSession(json, amount, currency) ?: return null

        return CheckoutSessionResponse(
            id = id,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
        )
    }

    /**
     * Parses the elements_session object if present.
     */
    private fun parseElementsSession(
        json: JSONObject,
        amount: Long,
        currency: String,
    ): ElementsSession? {
        val elementsSessionJson = json.optJSONObject(FIELD_ELEMENTS_SESSION) ?: return null
        return ElementsSessionJsonParser(
            params = elementsSessionParams.copy(
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
                )
            ),
            isLiveMode = isLiveMode,
        ).parse(elementsSessionJson)
    }

    /**
     * Creates a minimal ElementsSession for confirm responses that don't include elements_session.
     * Uses the PaymentIntent from the confirm response as the stripeIntent.
     */
    private fun createMinimalElementsSession(paymentIntent: PaymentIntent): ElementsSession {
        return ElementsSession.createFromFallback(
            stripeIntent = paymentIntent,
            sessionsError = null,
        )
    }

    /**
     * Extracts amount from `line_item_group.total` in the JSON response.
     */
    private fun extractAmount(json: JSONObject): Long? {
        val lineItemGroup = json.optJSONObject(FIELD_LINE_ITEM_GROUP) ?: return null
        val total = lineItemGroup.optLong(FIELD_TOTAL, -1)
        return if (total >= 0) total else null
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_ELEMENTS_SESSION = "elements_session"
        private const val FIELD_LINE_ITEM_GROUP = "line_item_group"
        private const val FIELD_TOTAL = "total"
        private const val FIELD_PAYMENT_INTENT = "payment_intent"
    }
}
