package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Card
import com.stripe.android.model.CardFunding
import com.stripe.android.model.TokenizationMethod
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardJsonParser : ModelJsonParser<Card> {
    override fun parse(json: JSONObject): Card? {
        if (VALUE_CARD != json.optString(FIELD_OBJECT)) {
            return null
        }

        // It's okay for the month to be missing, but not for it to be outside 1-12.
        // We treat an invalid month the same way we would an invalid brand, by reading it as
        // null.
        val expMonth = (StripeJsonUtils.optInteger(json, FIELD_EXP_MONTH) ?: -1)
            .takeUnless { it < 1 || it > 12 }
        val expYear = (StripeJsonUtils.optInteger(json, FIELD_EXP_YEAR) ?: -1)
            .takeUnless { it < 0 }

        // Note that we'll never get the CVC or card number in JSON, so those values are null
        return Card(
            expMonth = expMonth,
            expYear = expYear,
            addressCity = StripeJsonUtils.optString(json, FIELD_ADDRESS_CITY),
            addressLine1 = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1),
            addressLine1Check = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1_CHECK),
            addressLine2 = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE2),
            addressCountry = StripeJsonUtils.optString(json, FIELD_ADDRESS_COUNTRY),
            addressState = StripeJsonUtils.optString(json, FIELD_ADDRESS_STATE),
            addressZip = StripeJsonUtils.optString(json, FIELD_ADDRESS_ZIP),
            addressZipCheck = StripeJsonUtils.optString(json, FIELD_ADDRESS_ZIP_CHECK),
            brand = Card.getCardBrand(StripeJsonUtils.optString(json, FIELD_BRAND)),
            country = StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY),
            customerId = StripeJsonUtils.optString(json, FIELD_CUSTOMER),
            currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY),
            cvcCheck = StripeJsonUtils.optString(json, FIELD_CVC_CHECK),
            funding = CardFunding.fromCode(StripeJsonUtils.optString(json, FIELD_FUNDING)),
            fingerprint = StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
            id = StripeJsonUtils.optString(json, FIELD_ID),
            last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
            name = StripeJsonUtils.optString(json, FIELD_NAME),
            tokenizationMethod = TokenizationMethod.fromCode(
                StripeJsonUtils.optString(json, FIELD_TOKENIZATION_METHOD)
            )
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        internal const val VALUE_CARD = "card"

        private const val FIELD_OBJECT = "object"
        private const val FIELD_ADDRESS_CITY = "address_city"
        private const val FIELD_ADDRESS_COUNTRY = "address_country"
        private const val FIELD_ADDRESS_LINE1 = "address_line1"
        private const val FIELD_ADDRESS_LINE1_CHECK = "address_line1_check"
        private const val FIELD_ADDRESS_LINE2 = "address_line2"
        private const val FIELD_ADDRESS_STATE = "address_state"
        private const val FIELD_ADDRESS_ZIP = "address_zip"
        private const val FIELD_ADDRESS_ZIP_CHECK = "address_zip_check"
        private const val FIELD_BRAND = "brand"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_CVC_CHECK = "cvc_check"
        private const val FIELD_EXP_MONTH = "exp_month"
        private const val FIELD_EXP_YEAR = "exp_year"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_FUNDING = "funding"
        private const val FIELD_NAME = "name"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_ID = "id"
        private const val FIELD_TOKENIZATION_METHOD = "tokenization_method"
    }
}
