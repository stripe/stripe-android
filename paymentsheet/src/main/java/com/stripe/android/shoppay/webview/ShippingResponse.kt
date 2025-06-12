package com.stripe.android.shoppay.webview

import com.stripe.android.paymentsheet.WalletConfiguration
import com.stripe.android.shoppay.bridge.JsonSerializer
import org.json.JSONArray
import org.json.JSONObject

data class ShippingResponse(
    val merchantDecision: String,
    val lineItems: List<WalletConfiguration.LineItem>?,
    val shippingRates: List<WalletConfiguration.ShippingRate>?,
    val totalAmount: Int?,
): JsonSerializer {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("merchantDecision", merchantDecision)
            putOpt("lineItems", JSONArray(lineItems?.map { it.toJson() }))
            putOpt("shippingRates", JSONArray(shippingRates?.map { it.toJson() }))
            putOpt("totalAmount", totalAmount)
        }
    }
}
