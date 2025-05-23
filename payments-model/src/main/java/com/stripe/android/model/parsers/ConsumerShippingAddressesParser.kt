package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.ConsumerShippingAddressesResponse
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerShippingAddressesParser : ModelJsonParser<ConsumerShippingAddressesResponse> {

    override fun parse(json: JSONObject): ConsumerShippingAddressesResponse? {
        val shippingAddresses = json.optJSONArray("shipping_addresses") ?: return null
        val addresses = (0 until shippingAddresses.length()).mapNotNull { index ->
            val jsonObject = shippingAddresses.getJSONObject(index)
            parseShippingAddress(jsonObject)
        }
        return ConsumerShippingAddressesResponse(addresses)
    }

    private fun parseShippingAddress(json: JSONObject): ConsumerShippingAddress? {
        val id = json.optString("id")
        val isDefault = json.optBoolean("is_default")
        val address = json.optJSONObject("address") ?: return null

        return ConsumerShippingAddress(
            id = id,
            isDefault = isDefault,
            address = parseAddress(address),
        )
    }

    private fun parseAddress(json: JSONObject): ConsumerPaymentDetails.BillingAddress {
        return ConsumerPaymentDetails.BillingAddress(
            name = optString(json, "name"),
            line1 = optString(json, "line_1"),
            line2 = optString(json, "line_2"),
            locality = optString(json, "locality"),
            administrativeArea = optString(json, "administrative_area"),
            postalCode = optString(json, "postal_code"),
            countryCode = optString(json, "country_code")?.let { CountryCode(it) },
        )
    }
}
