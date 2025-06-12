package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

/**
 * Parser for shipping rate change request JSON messages.
 * Follows the same pattern as ShippingCalculationRequestJsonParser.
 */
internal class ShippingRateChangeRequestJsonParser : ModelJsonParser<ShippingRateChangeRequest> {

    override fun parse(json: JSONObject): ShippingRateChangeRequest? {
        val requestId = StripeJsonUtils.optString(json, FIELD_REQUEST_ID) ?: return null
        val timestamp = json.optLong(FIELD_TIMESTAMP, 0L)
        val currentAmount = json.optLong(FIELD_CURRENT_AMOUNT, 0L)

        val shippingRateJson = json.optJSONObject(FIELD_SHIPPING_RATE) ?: return null
        val shippingRate = parseShippingRate(shippingRateJson) ?: return null

        return ShippingRateChangeRequest(
            requestId = requestId,
            shippingRate = shippingRate,
            currentAmount = currentAmount,
            timestamp = timestamp
        )
    }

    private fun parseShippingRate(json: JSONObject): ShippingRate? {
        val id = StripeJsonUtils.optString(json, FIELD_ID) ?: return null
        val displayName = StripeJsonUtils.optString(json, FIELD_DISPLAY_NAME) ?: return null
        val amount = json.optLong(FIELD_AMOUNT, 0L)
        val deliveryEstimate = StripeJsonUtils.optString(json, FIELD_DELIVERY_ESTIMATE)

        return ShippingRate(
            id = id,
            displayName = displayName,
            amount = amount,
            deliveryEstimate = deliveryEstimate
        )
    }

    private companion object {
        private const val FIELD_REQUEST_ID = "requestId"
        private const val FIELD_SHIPPING_RATE = "shippingRate"
        private const val FIELD_CURRENT_AMOUNT = "currentAmount"
        private const val FIELD_TIMESTAMP = "timestamp"

        // Shipping rate fields
        private const val FIELD_ID = "id"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_DELIVERY_ESTIMATE = "deliveryEstimate"
    }
}
