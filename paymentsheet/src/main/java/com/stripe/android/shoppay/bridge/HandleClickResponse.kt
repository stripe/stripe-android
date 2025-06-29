package com.stripe.android.shoppay.bridge

import org.json.JSONArray
import org.json.JSONObject

internal data class HandleClickResponse(
    val lineItems: List<ECELineItem>?,
    val shippingRates: List<ECEShippingRate>?,
    val billingAddressRequired: Boolean?,
    val emailRequired: Boolean?,
    val phoneNumberRequired: Boolean?,
    val shippingAddressRequired: Boolean?,
    val allowedShippingCountries: List<String>?,
    val businessName: String,
    val shopId: String
) : JsonSerializer {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt("lineItems", lineItems?.let { JSONArray(it.map { item -> item.toJson() }) })
            putOpt("shippingRates", shippingRates?.let { JSONArray(it.map { rate -> rate.toJson() }) })
            putOpt("billingAddressRequired", billingAddressRequired)
            putOpt("emailRequired", emailRequired)
            putOpt("phoneNumberRequired", phoneNumberRequired)
            putOpt("shippingAddressRequired", shippingAddressRequired)
            putOpt("allowedShippingCountries", allowedShippingCountries?.let { JSONArray(it) })
            put(
                "business",
                JSONObject().apply {
                    put("name", businessName)
                }
            )
            put("shopId", shopId)
        }
    }
}
