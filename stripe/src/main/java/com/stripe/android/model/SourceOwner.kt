package com.stripe.android.model

import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [owner](https://stripe.com/docs/api#source_object-owner) object
 * in the Source api.
 */
data class SourceOwner constructor(
    val address: Address?,
    val email: String?,
    val name: String?,
    val phone: String?,
    val verifiedAddress: Address?,
    val verifiedEmail: String?,
    val verifiedName: String?,
    val verifiedPhone: String?
) : StripeModel() {
    companion object {

        private const val VERIFIED = "verified_"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_VERIFIED_ADDRESS = VERIFIED + FIELD_ADDRESS
        private const val FIELD_VERIFIED_EMAIL = VERIFIED + FIELD_EMAIL
        private const val FIELD_VERIFIED_NAME = VERIFIED + FIELD_NAME
        private const val FIELD_VERIFIED_PHONE = VERIFIED + FIELD_PHONE

        @JvmStatic
        fun fromString(jsonString: String?): SourceOwner? {
            if (jsonString == null) {
                return null
            }
            return try {
                fromJson(JSONObject(jsonString))
            } catch (ignored: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceOwner? {
            if (jsonObject == null) {
                return null
            }

            val address: Address?
            val addressJsonOpt = jsonObject.optJSONObject(FIELD_ADDRESS)
            address = if (addressJsonOpt != null) {
                Address.fromJson(addressJsonOpt)
            } else {
                null
            }

            val email = optString(jsonObject, FIELD_EMAIL)
            val name = optString(jsonObject, FIELD_NAME)
            val phone = optString(jsonObject, FIELD_PHONE)

            val verifiedAddressJsonOpt = jsonObject.optJSONObject(FIELD_VERIFIED_ADDRESS)
            val verifiedAddress = if (verifiedAddressJsonOpt != null) {
                Address.fromJson(verifiedAddressJsonOpt)
            } else {
                null
            }

            val verifiedEmail = optString(jsonObject, FIELD_VERIFIED_EMAIL)
            val verifiedName = optString(jsonObject, FIELD_VERIFIED_NAME)
            val verifiedPhone = optString(jsonObject, FIELD_VERIFIED_PHONE)

            return SourceOwner(
                address,
                email,
                name,
                phone,
                verifiedAddress,
                verifiedEmail,
                verifiedName,
                verifiedPhone
            )
        }
    }
}
