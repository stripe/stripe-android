package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class ShippingRateChangeRequestJsonParser @Inject constructor(
    private val shippingRateParser: ModelJsonParser<ECEShippingRate>
) : ModelJsonParser<ShippingRateChangeRequest> {

    override fun parse(json: JSONObject): ShippingRateChangeRequest? {
        val shippingRateJson = json.optJSONObject(FIELD_SHIPPING_RATE) ?: return null
        val shippingRate = shippingRateParser.parse(shippingRateJson) ?: return null

        return ShippingRateChangeRequest(
            shippingRate = shippingRate,
        )
    }

    private companion object {
        private const val FIELD_SHIPPING_RATE = "shippingRate"
    }
}
