package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class ECEShippingRateJsonParser @Inject constructor() : ModelJsonParser<ECEShippingRate> {

    override fun parse(json: JSONObject): ECEShippingRate? {
        val id = StripeJsonUtils.optString(json, FIELD_ID) ?: return null
        val displayName = StripeJsonUtils.optString(json, FIELD_DISPLAY_NAME) ?: return null
        val amount = json.optInt(FIELD_AMOUNT, 0)
        val deliveryEstimate = parseDeliveryEstimate(json, FIELD_DELIVERY_ESTIMATE)

        return ECEShippingRate(
            id = id,
            displayName = displayName,
            amount = amount,
            deliveryEstimate = deliveryEstimate
        )
    }

    private fun parseDeliveryEstimate(json: JSONObject, fieldName: String): ECEDeliveryEstimate? {
        if (!json.isNull(fieldName)) {
            val value = json.opt(fieldName)
            if (value is String && value.isNotEmpty()) {
                return ECEDeliveryEstimate.Text(value)
            }
        }

        val objectValue = json.optJSONObject(fieldName) ?: return null

        val maxJson = objectValue.optJSONObject(FIELD_MAXIMUM)
        val minJson = objectValue.optJSONObject(FIELD_MINIMUM)
        if (maxJson != null && minJson != null) {
            val maximum = parseDeliveryEstimateUnit(maxJson)
            val minimum = parseDeliveryEstimateUnit(minJson)
            if (maximum != null && minimum != null) {
                val structuredEstimate = ECEStructuredDeliveryEstimate(maximum, minimum)
                return ECEDeliveryEstimate.Range(structuredEstimate)
            }
        }

        return null
    }

    private fun parseDeliveryEstimateUnit(json: JSONObject): ECEDeliveryEstimateUnit? {
        val unitString = json.optString(FIELD_UNIT)
        val value = json.optInt(FIELD_VALUE, -1)

        if (value == -1 || unitString.isEmpty()) return null

        val unit = DeliveryTimeUnit.valueOf(unitString.uppercase())

        return ECEDeliveryEstimateUnit(unit, value)
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_DELIVERY_ESTIMATE = "deliveryEstimate"
        private const val FIELD_MAXIMUM = "maximum"
        private const val FIELD_MINIMUM = "minimum"
        private const val FIELD_UNIT = "unit"
        private const val FIELD_VALUE = "value"
    }
}
