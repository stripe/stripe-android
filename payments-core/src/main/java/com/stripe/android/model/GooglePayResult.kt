package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.model.parsers.TokenJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Result of a successful Google Pay Payment Data Request
 */
@Parcelize
data class GooglePayResult internal constructor(
    val token: Token? = null,
    val address: Address? = null,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val shippingInformation: ShippingInformation? = null
) : Parcelable {
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
            val stripeToken = TokenJsonParser().parse(JSONObject(paymentToken))

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
                phoneNumber = phone,
                shippingInformation = createShippingInformation(paymentDataJson)
            )
        }

        private fun createShippingInformation(
            paymentDataJson: JSONObject
        ): ShippingInformation? {
            return paymentDataJson.optJSONObject("shippingAddress")?.let {
                ShippingInformation(
                    address = Address(
                        line1 = optString(it, "address1"),
                        line2 = optString(it, "address2"),
                        postalCode = optString(it, "postalCode"),
                        city = optString(it, "locality"),
                        state = optString(it, "administrativeArea"),
                        country = optString(it, "countryCode")
                    ),
                    name = optString(it, "name"),
                    phone = optString(it, "phoneNumber")
                )
            }
        }
    }
}
