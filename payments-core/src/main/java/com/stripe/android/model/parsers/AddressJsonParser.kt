package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Address
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class AddressJsonParser : ModelJsonParser<Address> {
    override fun parse(json: JSONObject): Address {
        val city = StripeJsonUtils.optString(json, FIELD_CITY)
        val country = StripeJsonUtils.optString(json, FIELD_COUNTRY)
        val line1 = StripeJsonUtils.optString(json, FIELD_LINE_1)
        val line2 = StripeJsonUtils.optString(json, FIELD_LINE_2)
        val postalCode = StripeJsonUtils.optString(json, FIELD_POSTAL_CODE)
        val state = StripeJsonUtils.optString(json, FIELD_STATE)
        return Address(city, country, line1, line2, postalCode, state)
    }

    private companion object {
        private const val FIELD_CITY = "city"

        // 2 Character Country Code
        private const val FIELD_COUNTRY = "country"

        private const val FIELD_LINE_1 = "line1"
        private const val FIELD_LINE_2 = "line2"
        private const val FIELD_POSTAL_CODE = "postal_code"
        private const val FIELD_STATE = "state"
    }
}
