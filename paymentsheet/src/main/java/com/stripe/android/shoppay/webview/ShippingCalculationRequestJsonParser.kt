package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

/**
 * Parser for shipping calculation request JSON messages.
 * Follows the same pattern as ElementsSessionJsonParser.
 */
internal class ShippingCalculationRequestJsonParser : ModelJsonParser<ShippingCalculationRequest> {
    
    override fun parse(json: JSONObject): ShippingCalculationRequest? {
        val requestId = StripeJsonUtils.optString(json, FIELD_REQUEST_ID) ?: return null
        val timestamp = json.optLong(FIELD_TIMESTAMP, 0L)
        
        val shippingAddressJson = json.optJSONObject(FIELD_SHIPPING_ADDRESS) ?: return null
        val shippingAddress = parseShippingAddress(shippingAddressJson) ?: return null
        
        return ShippingCalculationRequest(
            requestId = requestId,
            shippingAddress = shippingAddress,
            timestamp = timestamp
        )
    }
    
    private fun parseShippingAddress(json: JSONObject): ShippingAddress? {
        return ShippingAddress(
            address1 = StripeJsonUtils.optString(json, FIELD_ADDRESS1),
            address2 = StripeJsonUtils.optString(json, FIELD_ADDRESS2),
            city = StripeJsonUtils.optString(json, FIELD_CITY),
            companyName = StripeJsonUtils.optString(json, FIELD_COMPANY_NAME),
            countryCode = StripeJsonUtils.optString(json, FIELD_COUNTRY_CODE),
            email = StripeJsonUtils.optString(json, FIELD_EMAIL),
            firstName = StripeJsonUtils.optString(json, FIELD_FIRST_NAME),
            lastName = StripeJsonUtils.optString(json, FIELD_LAST_NAME),
            phone = StripeJsonUtils.optString(json, FIELD_PHONE),
            postalCode = StripeJsonUtils.optString(json, FIELD_POSTAL_CODE),
            provinceCode = StripeJsonUtils.optString(json, FIELD_PROVINCE_CODE)
        )
    }
    
    private companion object {
        private const val FIELD_REQUEST_ID = "requestId"
        private const val FIELD_SHIPPING_ADDRESS = "shippingAddress"
        private const val FIELD_TIMESTAMP = "timestamp"
        
        // Shipping address fields
        private const val FIELD_ADDRESS1 = "address1"
        private const val FIELD_ADDRESS2 = "address2"
        private const val FIELD_CITY = "city"
        private const val FIELD_COMPANY_NAME = "companyName"
        private const val FIELD_COUNTRY_CODE = "countryCode"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_FIRST_NAME = "firstName"
        private const val FIELD_LAST_NAME = "lastName"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_PROVINCE_CODE = "provinceCode"
    }
} 