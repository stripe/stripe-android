package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

/**
 * Parser for confirm payment request JSON messages.
 */
internal class ConfirmPaymentRequestJsonParser : ModelJsonParser<ConfirmPaymentRequest> {

    override fun parse(json: JSONObject): ConfirmPaymentRequest? {
        val requestId = StripeJsonUtils.optString(json, FIELD_REQUEST_ID) ?: return null
        val timestamp = json.optLong(FIELD_TIMESTAMP, 0L)

        val paymentDetailsJson = json.optJSONObject(FIELD_PAYMENT_DETAILS) ?: return null
        val paymentDetails = parsePaymentDetails(paymentDetailsJson) ?: return null

        return ConfirmPaymentRequest(
            requestId = requestId,
            paymentDetails = paymentDetails,
            timestamp = timestamp
        )
    }

    private fun parsePaymentDetails(json: JSONObject): ConfirmPaymentRequest.PaymentDetails? {
        val billingDetailsJson = json.optJSONObject(FIELD_BILLING_DETAILS) ?: return null
        val billingDetails = parseBillingDetails(billingDetailsJson) ?: return null

        val shippingAddressJson = json.optJSONObject(FIELD_SHIPPING_ADDRESS) ?: return null
        val shippingAddress = parseShippingAddress(shippingAddressJson) ?: return null

        val shippingRateJson = json.optJSONObject(FIELD_SHIPPING_RATE) ?: return null
        val shippingRate = parseShippingRate(shippingRateJson) ?: return null

        val mode = StripeJsonUtils.optString(json, FIELD_MODE) ?: return null
        val captureMethod = StripeJsonUtils.optString(json, FIELD_CAPTURE_METHOD) ?: return null
        val paymentMethod = StripeJsonUtils.optString(json, FIELD_PAYMENT_METHOD)
        val createPaymentMethodEnabled = json.optBoolean(FIELD_CREATE_PAYMENT_METHOD_ENABLED, false)
        val amount = json.optLong(FIELD_AMOUNT, 0L)

        return ConfirmPaymentRequest.PaymentDetails(
            billingDetails = billingDetails,
            shippingAddress = shippingAddress,
            shippingRate = shippingRate,
            mode = mode,
            captureMethod = captureMethod,
            paymentMethod = paymentMethod,
            createPaymentMethodEnabled = createPaymentMethodEnabled,
            amount = amount
        )
    }

    private fun parseBillingDetails(json: JSONObject): ConfirmPaymentRequest.BillingDetails? {
        val addressJson = json.optJSONObject(FIELD_ADDRESS) ?: return null
        val address = parseAddress(addressJson) ?: return null

        val email = StripeJsonUtils.optString(json, FIELD_EMAIL) ?: return null
        val name = StripeJsonUtils.optString(json, FIELD_NAME) ?: return null
        val phone = StripeJsonUtils.optString(json, FIELD_PHONE) ?: return null

        return ConfirmPaymentRequest.BillingDetails(
            address = address,
            email = email,
            name = name,
            phone = phone
        )
    }

    private fun parseAddress(json: JSONObject): ConfirmPaymentRequest.Address? {
        val city = StripeJsonUtils.optString(json, FIELD_CITY) ?: return null
        val country = StripeJsonUtils.optString(json, FIELD_COUNTRY) ?: return null
        val line1 = StripeJsonUtils.optString(json, FIELD_LINE1) ?: return null
        val postalCode = StripeJsonUtils.optString(json, FIELD_POSTAL_CODE) ?: return null
        val state = StripeJsonUtils.optString(json, FIELD_STATE) ?: return null

        return ConfirmPaymentRequest.Address(
            city = city,
            country = country,
            line1 = line1,
            postalCode = postalCode,
            state = state
        )
    }

    private fun parseShippingAddress(json: JSONObject): ConfirmPaymentRequest.ShippingAddress? {
        val city = StripeJsonUtils.optString(json, FIELD_CITY) ?: return null
        val country = StripeJsonUtils.optString(json, FIELD_COUNTRY) ?: return null
        val line = json.optJSONArray(FIELD_LINE)?.let { array ->
            List(array.length()) { i -> array.getString(i) }
        } ?: return null
        val postalCode = StripeJsonUtils.optString(json, FIELD_POSTAL_CODE) ?: return null
        val state = StripeJsonUtils.optString(json, FIELD_STATE) ?: return null

        return ConfirmPaymentRequest.ShippingAddress(
            city = city,
            country = country,
            line = line,
            postalCode = postalCode,
            state = state
        )
    }

    private fun parseShippingRate(json: JSONObject): ConfirmPaymentRequest.ShippingRate? {
        val id = StripeJsonUtils.optString(json, FIELD_ID) ?: return null
        val displayName = StripeJsonUtils.optString(json, FIELD_DISPLAY_NAME) ?: return null
        val amount = json.optLong(FIELD_AMOUNT, 0L)

        return ConfirmPaymentRequest.ShippingRate(
            id = id,
            displayName = displayName,
            amount = amount
        )
    }

    private companion object {
        private const val FIELD_REQUEST_ID = "requestId"
        private const val FIELD_PAYMENT_DETAILS = "paymentDetails"
        private const val FIELD_TIMESTAMP = "timestamp"

        // Payment details fields
        private const val FIELD_BILLING_DETAILS = "billingDetails"
        private const val FIELD_SHIPPING_ADDRESS = "shippingAddress"
        private const val FIELD_SHIPPING_RATE = "shippingRate"
        private const val FIELD_MODE = "mode"
        private const val FIELD_CAPTURE_METHOD = "captureMethod"
        private const val FIELD_PAYMENT_METHOD = "paymentMethod"
        private const val FIELD_CREATE_PAYMENT_METHOD_ENABLED = "createPaymentMethodEnabled"
        private const val FIELD_AMOUNT = "amount"

        // Billing details fields
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"

        // Address fields
        private const val FIELD_CITY = "city"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_LINE1 = "line1"
        private const val FIELD_LINE = "line"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_STATE = "state"

        // Shipping rate fields
        private const val FIELD_ID = "id"
        private const val FIELD_DISPLAY_NAME = "displayName"
    }
}
