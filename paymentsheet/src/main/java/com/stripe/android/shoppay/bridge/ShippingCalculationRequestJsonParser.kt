package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

internal class ShippingCalculationRequestJsonParser @Inject constructor() :
    ModelJsonParser<ShippingCalculationRequest> {

    override fun parse(json: JSONObject): ShippingCalculationRequest? {
        val shippingAddressJson = json.optJSONObject(FIELD_SHIPPING_ADDRESS) ?: return null
        val shippingAddress = parseShippingAddress(shippingAddressJson) ?: return null

        return ShippingCalculationRequest(
            shippingAddress = shippingAddress,
        )
    }

    private fun parseShippingAddress(json: JSONObject): ShippingCalculationRequest.ShippingAddress? {
        val addressJson = json.optJSONObject(FIELD_ADDRESS) ?: return null

        val name = StripeJsonUtils.optString(json, FIELD_NAME)
        val addressLine = parseAddressLine(addressJson)

        val partialAddress = ECEPartialAddress(
            addressLine = addressLine,
            city = StripeJsonUtils.optString(addressJson, FIELD_CITY),
            state = StripeJsonUtils.optString(addressJson, FIELD_STATE),
            postalCode = StripeJsonUtils.optString(addressJson, FIELD_POSTAL_CODE),
            country = StripeJsonUtils.optString(addressJson, FIELD_COUNTRY),
            phone = StripeJsonUtils.optString(addressJson, FIELD_PHONE),
            organization = StripeJsonUtils.optString(addressJson, FIELD_ORGANIZATION)
        )

        return ShippingCalculationRequest.ShippingAddress(
            name = name,
            address = partialAddress
        )
    }

    private fun parseAddressLine(addressJson: JSONObject?): List<String>? {
        val addressLine = addressJson?.opt(FIELD_ADDRESS_LINE)
        if (addressLine is String) {
            return listOf(addressLine)
        } else if (addressLine is JSONArray) {
            return StripeJsonUtils.jsonArrayToList(addressLine)?.mapNotNull {
                it as? String
            }
        }
        return null
    }

    private companion object {
        private const val FIELD_SHIPPING_ADDRESS = "shippingAddress"

        // Shipping address fields
        private const val FIELD_NAME = "name"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_ADDRESS_LINE = "addressLine"
        private const val FIELD_CITY = "city"
        private const val FIELD_STATE = "state"
        private const val FIELD_POSTAL_CODE = "postal_code"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_ORGANIZATION = "organization"
    }
}
