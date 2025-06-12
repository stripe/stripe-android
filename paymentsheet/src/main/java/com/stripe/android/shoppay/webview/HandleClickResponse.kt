package com.stripe.android.shoppay.webview

import com.stripe.android.paymentsheet.WalletConfiguration
import com.stripe.android.shoppay.bridge.JsonSerializer
import org.json.JSONArray
import org.json.JSONObject

data class HandleClickResponse(
    val lineItems: List<WalletConfiguration.LineItem>?,
    val shippingRates: List<WalletConfiguration.ShippingRate>?,
    val billingAddressRequired: Boolean?,
    val emailRequired: Boolean?,
    val phoneNumberRequired: Boolean?,
    val shippingAddressRequired: Boolean?,
    val allowedShippingCountries: List<String>?,
    val disableOverlay: Boolean?
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
            putOpt("disableOverlay", disableOverlay)
        }
    }
}
