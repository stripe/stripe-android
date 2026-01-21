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
    private val elementsSessionParams: ElementsSessionParams.CheckoutSessionType,
    private val isLiveMode: Boolean,
) : ModelJsonParser<CheckoutSessionResponse> {

    override fun parse(json: JSONObject): CheckoutSessionResponse? {
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return null
        val amount = extractAmount(json) ?: return null
        val currency = json.optString(FIELD_CURRENCY).takeIf { it.isNotEmpty() } ?: return null

        val elementsSessionJson = json.optJSONObject(FIELD_ELEMENTS_SESSION) ?: return null
        val elementsSession = ElementsSessionJsonParser(
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
        ).parse(elementsSessionJson) ?: return null

        return CheckoutSessionResponse(
            id = id,
            amount = amount,
            currency = currency,
            elementsSession = elementsSession,
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
    }
}
