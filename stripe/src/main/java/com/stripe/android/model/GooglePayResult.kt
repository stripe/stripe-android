package com.stripe.android.model

import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Result of a successful Google Pay Payment Data Request
 */
data class GooglePayResult constructor(
    val token: Token? = null,
    val address: Address? = null,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null
) {
    companion object {
        /**
         * @param paymentDataJson a JSONObject representation of a
         * [PaymentData](https://developers.google.com/pay/api/android/reference/response-objects#PaymentData)
         * response
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJson(paymentDataJson: JSONObject): GooglePayResult {
            val paymentMethodData = paymentDataJson
                .getJSONObject("paymentMethodData")
            val paymentToken = paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token")
            val stripeToken = Token.fromJson(JSONObject(paymentToken))

            val googlePayBillingAddress = paymentMethodData
                .getJSONObject("info")
                .optJSONObject("billingAddress")

            val address = googlePayBillingAddress?.let {
                Address(
                    line1 = optString(it, "address1"),
                    line2 = optString(it, "address2"),
                    city = optString(it, "locality"),
                    state = optString(it, "administrativeArea"),
                    country = optString(it, "countryCode"),
                    postalCode = optString(it, "postalCode")
                )
            }

            val name = optString(googlePayBillingAddress, "name")
            val email = optString(paymentDataJson, "email")
            val phone = optString(googlePayBillingAddress, "phoneNumber")

            return GooglePayResult(
                token = stripeToken,
                address = address,
                name = name,
                email = email,
                phoneNumber = phone
            )
        }
    }
}
