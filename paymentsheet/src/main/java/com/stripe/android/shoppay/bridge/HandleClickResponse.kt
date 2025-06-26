package com.stripe.android.shoppay.bridge

import com.stripe.android.paymentsheet.PaymentSheet
import org.json.JSONArray
import org.json.JSONObject

internal data class HandleClickResponse(
    val lineItems: List<PaymentSheet.ShopPayConfiguration.LineItem>?,
    val shippingRates: List<PaymentSheet.ShopPayConfiguration.ShippingRate>?,
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
            putOpt("lineItems", lineItems?.let { JSONArray(it.map { item -> item.toJSON() }) })
            putOpt("shippingRates", shippingRates?.let { JSONArray(it.map { rate -> rate.toJSON() }) })
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
