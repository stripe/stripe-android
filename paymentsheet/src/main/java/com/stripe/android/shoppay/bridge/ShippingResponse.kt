package com.stripe.android.shoppay.bridge

import org.json.JSONArray
import org.json.JSONObject

internal data class ShippingResponse(
    val lineItems: List<ECELineItem>?,
    val shippingRates: List<ECEShippingRate>?,
    val totalAmount: Int?,
) : JsonSerializer {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt("lineItems", JSONArray(lineItems?.map { it.toJson() }))
            putOpt("shippingRates", JSONArray(shippingRates?.map { it.toJson() }))
            putOpt("totalAmount", totalAmount)
        }
    }
}
