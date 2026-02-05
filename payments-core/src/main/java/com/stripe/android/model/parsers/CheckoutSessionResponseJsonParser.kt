package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

/**
 * Parser for checkout session API responses:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns elements_session
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns payment_intent
 *
 * The init response contains checkout session metadata (`id`, `amount`, `currency`) and an
 * embedded `elements_session` object. The confirm response contains a `payment_intent` object.
 *
 * For init responses, customer data comes from the top-level `customer` field (NOT inside
 * elements_session), since checkout sessions associate customers server-side.
 *
 * For confirm responses, this parser extracts the `payment_intent` and creates a minimal
 * response with the payment intent data.
 */
internal class CheckoutSessionResponseJsonParser(
    private val elementsSessionParams: ElementsSessionParams.CheckoutSessionType,
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        val sessionId = json.optString(FIELD_SESSION_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = extractDueAmount(json) ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null
        val paymentIntent = json.optJSONObject(FIELD_PAYMENT_INTENT)?.let {
            PaymentIntentJsonParser().parse(it)
        }
        val elementsSession = parseElementsSession(json, amount, currency)
        val customer = parseCustomer(json.optJSONObject(FIELD_CUSTOMER))

        return CheckoutSessionResponse(
            id = sessionId,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
            customer = customer,
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
     * Extracts amount from `total_summary.due` in response JSON.
     */
    private fun extractDueAmount(json: JSONObject): Long? {
        val totalSummary = json.optJSONObject(FIELD_TOTAL_SUMMARY) ?: return null
        val due = totalSummary.optLong(FIELD_DUE, -1)
        return if (due >= 0) due else null
    }

    /**
     * Parses the top-level customer object from checkout session init response.
     * Customer is associated server-side when the checkout session is created,
     * so we get customer data directly in the init response.
     *
     * Expected JSON structure:
     * ```json
     * {
     *   "customer": {
     *     "id": "cus_xxx",
     *     "payment_methods": [...],
     *     "default_payment_method": "pm_xxx" // optional
     *   }
     * }
     * ```
     */
    private fun parseCustomer(json: JSONObject?): CheckoutSessionResponse.Customer? {
        if (json == null) {
            return null
        }

        val customerId = json.optString(FIELD_CUSTOMER_ID).takeIf { it.isNotEmpty() } ?: return null
        val paymentMethodsJson = json.optJSONArray(FIELD_PAYMENT_METHODS)
        val paymentMethods = paymentMethodsJson?.let { pmsJson ->
            (0 until pmsJson.length()).mapNotNull { index ->
                PaymentMethodJsonParser().parse(pmsJson.optJSONObject(index))
            }
        } ?: emptyList()
        val defaultPaymentMethodId = json.optString(FIELD_DEFAULT_PAYMENT_METHOD).takeIf { it.isNotEmpty() }

        return CheckoutSessionResponse.Customer(
            id = customerId,
            paymentMethods = paymentMethods,
            defaultPaymentMethodId = defaultPaymentMethodId,
        )
    }

    private companion object {
        private const val FIELD_SESSION_ID = "session_id"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_ELEMENTS_SESSION = "elements_session"
        private const val FIELD_TOTAL_SUMMARY = "total_summary"
        private const val FIELD_DUE = "due"
        private const val FIELD_PAYMENT_INTENT = "payment_intent"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_CUSTOMER_ID = "id"
        private const val FIELD_PAYMENT_METHODS = "payment_methods"
        private const val FIELD_DEFAULT_PAYMENT_METHOD = "default_payment_method"
    }
}
