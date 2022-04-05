package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ShippingInformation
import org.json.JSONObject

internal class ShippingInformationJsonParser : ModelJsonParser<ShippingInformation> {
    override fun parse(json: JSONObject): ShippingInformation {
        return ShippingInformation(
            json.optJSONObject(FIELD_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            StripeJsonUtils.optString(json, FIELD_NAME),
            StripeJsonUtils.optString(json, FIELD_PHONE)
        )
    }

    private companion object {
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"
    }
}
