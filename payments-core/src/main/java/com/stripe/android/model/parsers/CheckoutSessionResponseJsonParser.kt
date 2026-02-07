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
 * For confirm responses, this parser extracts the `payment_intent` and creates a minimal
 * response with the payment intent data.
 */
internal class CheckoutSessionResponseJsonParser(
    private val elementsSessionParams: ElementsSessionParams.CheckoutSession.Initial,
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

        return CheckoutSessionResponse(
            id = sessionId,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
        )
    }

    /**
     * Parses the elements_session object if present.
     * Creates a [ElementsSessionParams.CheckoutSession.WithIntent] from the initial params
     * enriched with the [DeferredIntentParams] from the response.
     */
    private fun parseElementsSession(
        json: JSONObject,
        amount: Long,
        currency: String,
    ): ElementsSession? {
        val elementsSessionJson = json.optJSONObject(FIELD_ELEMENTS_SESSION) ?: return null

        // Create WithIntent params from Initial params + response data
        val withIntentParams = ElementsSessionParams.CheckoutSession.WithIntent(
            clientSecret = elementsSessionParams.clientSecret,
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
            locale = elementsSessionParams.locale,
            customerSessionClientSecret = elementsSessionParams.customerSessionClientSecret,
            legacyCustomerEphemeralKey = elementsSessionParams.legacyCustomerEphemeralKey,
            mobileSessionId = elementsSessionParams.mobileSessionId,
            savedPaymentMethodSelectionId = elementsSessionParams.savedPaymentMethodSelectionId,
            customPaymentMethods = elementsSessionParams.customPaymentMethods,
            externalPaymentMethods = elementsSessionParams.externalPaymentMethods,
            appId = elementsSessionParams.appId,
            sellerDetails = elementsSessionParams.sellerDetails,
            link = elementsSessionParams.link,
            countryOverride = elementsSessionParams.countryOverride,
        )

        return ElementsSessionJsonParser(
            params = withIntentParams,
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
    }
}
