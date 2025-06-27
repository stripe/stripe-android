package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class ConfirmationRequestJsonParser @Inject constructor(
    private val shippingRateParser: ModelJsonParser<ECEShippingRate>
) : ModelJsonParser<ConfirmationRequest> {

    override fun parse(json: JSONObject): ConfirmationRequest? {
        val paymentDetailsJson = json.optJSONObject(FIELD_PAYMENT_DETAILS) ?: return null
        val paymentDetails = parsePaymentDetails(paymentDetailsJson) ?: return null

        return ConfirmationRequest(
            paymentDetails = paymentDetails,
        )
    }

    private fun parsePaymentDetails(json: JSONObject): ConfirmationRequest.ConfirmEventData? {
        val billingDetails = json.optJSONObject(FIELD_BILLING_DETAILS)?.let {
            parseBillingDetails(it)
        } ?: throw IllegalArgumentException("Missing billing details")

        val shippingAddress = json.optJSONObject(FIELD_SHIPPING_ADDRESS)?.let {
            parseShippingAddress(it)
        }
        val shippingRate = json.optJSONObject(FIELD_SHIPPING_RATE)?.let {
            shippingRateParser.parse(it)
        }
        val paymentMethodOptions = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS)?.let {
            parsePaymentMethodOptions(it)
        }

        return ConfirmationRequest.ConfirmEventData(
            billingDetails = billingDetails,
            shippingAddress = shippingAddress,
            shippingRate = shippingRate,
            paymentMethodOptions = paymentMethodOptions
        )
    }

    private fun parseBillingDetails(json: JSONObject): ECEBillingDetails? {
        val name = StripeJsonUtils.optString(json, FIELD_BILLING_NAME)
        val email = StripeJsonUtils.optString(json, FIELD_BILLING_EMAIL)
        val phone = StripeJsonUtils.optString(json, FIELD_BILLING_PHONE)
        val address = json.optJSONObject(FIELD_BILLING_ADDRESS)?.let { parseFullAddress(it) }

        return ECEBillingDetails(
            name = name,
            email = email,
            phone = phone,
            address = address
        )
    }

    private fun parseShippingAddress(json: JSONObject): ECEShippingAddressData? {
        val name = StripeJsonUtils.optString(json, FIELD_SHIPPING_NAME)
        val address = json.optJSONObject(FIELD_SHIPPING_ADDRESS_DETAILS)?.let { parseFullAddress(it) }

        return ECEShippingAddressData(
            name = name,
            address = address
        )
    }

    private fun parseFullAddress(json: JSONObject): ECEFullAddress? {
        val line1 = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1)
        val line2 = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE2)
        val city = StripeJsonUtils.optString(json, FIELD_ADDRESS_CITY)
        val state = StripeJsonUtils.optString(json, FIELD_ADDRESS_STATE)
        val postalCode = StripeJsonUtils.optString(json, FIELD_ADDRESS_POSTAL_CODE)
        val country = StripeJsonUtils.optString(json, FIELD_ADDRESS_COUNTRY)

        return ECEFullAddress(
            line1 = line1,
            line2 = line2,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country
        )
    }

    private fun parsePaymentMethodOptions(json: JSONObject): ECEPaymentMethodOptions? {
        val shopPay = json.optJSONObject(FIELD_SHOP_PAY)?.let { parseShopPay(it) }

        return ECEPaymentMethodOptions(
            shopPay = shopPay
        )
    }

    private fun parseShopPay(json: JSONObject): ECEPaymentMethodOptions.ShopPay? {
        val externalSourceId = StripeJsonUtils.optString(json, FIELD_EXTERNAL_SOURCE_ID) ?: return null

        return ECEPaymentMethodOptions.ShopPay(
            externalSourceId = externalSourceId
        )
    }

    private companion object {
        private const val FIELD_PAYMENT_DETAILS = "paymentDetails"

        // Billing details fields
        private const val FIELD_BILLING_DETAILS = "billingDetails"
        private const val FIELD_BILLING_NAME = "name"
        private const val FIELD_BILLING_EMAIL = "email"
        private const val FIELD_BILLING_PHONE = "phone"
        private const val FIELD_BILLING_ADDRESS = "address"

        // Shipping address fields
        private const val FIELD_SHIPPING_ADDRESS = "shippingAddress"
        private const val FIELD_SHIPPING_NAME = "name"
        private const val FIELD_SHIPPING_ADDRESS_DETAILS = "address"

        // Address fields
        private const val FIELD_ADDRESS_LINE1 = "line1"
        private const val FIELD_ADDRESS_LINE2 = "line2"
        private const val FIELD_ADDRESS_CITY = "city"
        private const val FIELD_ADDRESS_STATE = "state"
        private const val FIELD_ADDRESS_POSTAL_CODE = "postalCode"
        private const val FIELD_ADDRESS_COUNTRY = "country"

        // Shipping rate fields
        private const val FIELD_SHIPPING_RATE = "shippingRate"

        // Payment method options fields
        private const val FIELD_PAYMENT_METHOD_OPTIONS = "paymentMethodOptions"
        private const val FIELD_SHOP_PAY = "shopPay"
        private const val FIELD_EXTERNAL_SOURCE_ID = "externalSourceId"
    }
}
