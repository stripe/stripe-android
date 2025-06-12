package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

/**
 * Parser for handle click request JSON messages.
 * Follows the same pattern as ShippingCalculationRequestJsonParser.
 */
internal class HandleClickRequestJsonParser : ModelJsonParser<HandleClickRequest> {

    override fun parse(json: JSONObject): HandleClickRequest? {
        val requestId = StripeJsonUtils.optString(json, FIELD_REQUEST_ID) ?: return null
        val timestamp = json.optLong(FIELD_TIMESTAMP, 0L)

        val eventDataJson = json.optJSONObject(FIELD_EVENT_DATA) ?: return null
        val eventData = parseEventData(eventDataJson) ?: return null

        return HandleClickRequest(
            requestId = requestId,
            eventData = eventData,
            timestamp = timestamp
        )
    }

    private fun parseEventData(json: JSONObject): EventData? {
        val expressPaymentType = StripeJsonUtils.optString(json, FIELD_EXPRESS_PAYMENT_TYPE) ?: return null

        return EventData(
            expressPaymentType = expressPaymentType
        )
    }

    private companion object {
        private const val FIELD_REQUEST_ID = "requestId"
        private const val FIELD_EVENT_DATA = "eventData"
        private const val FIELD_TIMESTAMP = "timestamp"

        // Event data fields
        private const val FIELD_EXPRESS_PAYMENT_TYPE = "expressPaymentType"
    }
}
