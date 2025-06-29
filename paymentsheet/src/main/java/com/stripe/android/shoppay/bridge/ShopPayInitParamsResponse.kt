package com.stripe.android.shoppay.bridge

import org.json.JSONObject

internal data class ShopPayInitParamsResponse(
    val shopId: String,
    val customerSessionClientSecret: String,
    val amountTotal: Int,
) : JsonSerializer {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("shopId", shopId)
            put("customerSessionClientSecret", customerSessionClientSecret)
            put("amountTotal", amountTotal)
        }
    }
}
