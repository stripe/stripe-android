package com.stripe.android.model.parsers

import com.stripe.android.model.Address
import com.stripe.android.model.SourceOwner
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class SourceOwnerJsonParser : ModelJsonParser<SourceOwner> {
    override fun parse(json: JSONObject): SourceOwner {
        val addressJsonOpt = json.optJSONObject(FIELD_ADDRESS)
        val address = if (addressJsonOpt != null) {
            Address.fromJson(addressJsonOpt)
        } else {
            null
        }

        val email = StripeJsonUtils.optString(json, FIELD_EMAIL)
        val name = StripeJsonUtils.optString(json, FIELD_NAME)
        val phone = StripeJsonUtils.optString(json, FIELD_PHONE)

        val verifiedAddressJsonOpt = json.optJSONObject(FIELD_VERIFIED_ADDRESS)
        val verifiedAddress = if (verifiedAddressJsonOpt != null) {
            Address.fromJson(verifiedAddressJsonOpt)
        } else {
            null
        }

        val verifiedEmail = StripeJsonUtils.optString(json, FIELD_VERIFIED_EMAIL)
        val verifiedName = StripeJsonUtils.optString(json, FIELD_VERIFIED_NAME)
        val verifiedPhone = StripeJsonUtils.optString(json, FIELD_VERIFIED_PHONE)

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

    private companion object {
        private const val VERIFIED = "verified_"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_VERIFIED_ADDRESS = VERIFIED + FIELD_ADDRESS
        private const val FIELD_VERIFIED_EMAIL = VERIFIED + FIELD_EMAIL
        private const val FIELD_VERIFIED_NAME = VERIFIED + FIELD_NAME
        private const val FIELD_VERIFIED_PHONE = VERIFIED + FIELD_PHONE
    }
}
