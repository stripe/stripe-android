package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Source
import org.json.JSONObject

internal class SourceOwnerJsonParser : ModelJsonParser<Source.Owner> {
    override fun parse(json: JSONObject): Source.Owner {
        return Source.Owner(
            address = json.optJSONObject(FIELD_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            email = StripeJsonUtils.optString(json, FIELD_EMAIL),
            name = StripeJsonUtils.optString(json, FIELD_NAME),
            phone = StripeJsonUtils.optString(json, FIELD_PHONE),
            verifiedAddress = json.optJSONObject(FIELD_VERIFIED_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            verifiedEmail = StripeJsonUtils.optString(json, FIELD_VERIFIED_EMAIL),
            verifiedName = StripeJsonUtils.optString(json, FIELD_VERIFIED_NAME),
            verifiedPhone = StripeJsonUtils.optString(json, FIELD_VERIFIED_PHONE)
        )
    }

    private companion object {
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_VERIFIED_ADDRESS = "verified_address"
        private const val FIELD_VERIFIED_EMAIL = "verified_email"
        private const val FIELD_VERIFIED_NAME = "verified_name"
        private const val FIELD_VERIFIED_PHONE = "verified_phone"
    }
}
