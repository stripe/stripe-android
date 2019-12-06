package com.stripe.android.model.parsers

import com.stripe.android.model.Card
import com.stripe.android.model.Card.CardBrand
import com.stripe.android.model.Card.FundingType
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class CardJsonParser : ModelJsonParser<Card> {
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
        return Card.Builder(null, expMonth, expYear, null)
            .addressCity(StripeJsonUtils.optString(json, FIELD_ADDRESS_CITY))
            .addressLine1(StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1))
            .addressLine1Check(StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1_CHECK))
            .addressLine2(StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE2))
            .addressCountry(StripeJsonUtils.optString(json, FIELD_ADDRESS_COUNTRY))
            .addressState(StripeJsonUtils.optString(json, FIELD_ADDRESS_STATE))
            .addressZip(StripeJsonUtils.optString(json, FIELD_ADDRESS_ZIP))
            .addressZipCheck(StripeJsonUtils.optString(json, FIELD_ADDRESS_ZIP_CHECK))
            .brand(asCardBrand(StripeJsonUtils.optString(json, FIELD_BRAND)))
            .country(StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY))
            .customer(StripeJsonUtils.optString(json, FIELD_CUSTOMER))
            .currency(StripeJsonUtils.optCurrency(json, FIELD_CURRENCY))
            .cvcCheck(StripeJsonUtils.optString(json, FIELD_CVC_CHECK))
            .funding(asFundingType(StripeJsonUtils.optString(json, FIELD_FUNDING)))
            .fingerprint(StripeJsonUtils.optString(json, FIELD_FINGERPRINT))
            .id(StripeJsonUtils.optString(json, FIELD_ID))
            .last4(StripeJsonUtils.optString(json, FIELD_LAST4))
            .name(StripeJsonUtils.optString(json, FIELD_NAME))
            .tokenizationMethod(StripeJsonUtils.optString(json, FIELD_TOKENIZATION_METHOD))
            .metadata(StripeJsonUtils.optHash(json, FIELD_METADATA))
            .build()
    }

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
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_NAME = "name"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_ID = "id"
        private const val FIELD_TOKENIZATION_METHOD = "tokenization_method"

        /**
         * Converts an unchecked String value to a [CardBrand] or `null`.
         *
         * @param possibleCardType a String that might match a [CardBrand] or be empty.
         * @return `null` if the input is blank, else the appropriate [CardBrand].
         */
        @JvmStatic
        @CardBrand
        fun asCardBrand(possibleCardType: String?): String? {
            if (possibleCardType.isNullOrBlank()) {
                return null
            }

            return when {
                CardBrand.AMERICAN_EXPRESS.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.AMERICAN_EXPRESS
                CardBrand.MASTERCARD.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.MASTERCARD
                CardBrand.DINERS_CLUB.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.DINERS_CLUB
                CardBrand.DISCOVER.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.DISCOVER
                CardBrand.JCB.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.JCB
                CardBrand.VISA.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.VISA
                CardBrand.UNIONPAY.equals(possibleCardType, ignoreCase = true) ->
                    CardBrand.UNIONPAY
                else -> CardBrand.UNKNOWN
            }
        }

        /**
         * Converts an unchecked String value to a [FundingType] or `null`.
         *
         * @param possibleFundingType a String that might match a [FundingType] or be empty
         * @return `null` if the input is blank, else the appropriate [FundingType]
         */
        @JvmStatic
        @FundingType
        fun asFundingType(possibleFundingType: String?): String? {
            if (possibleFundingType.isNullOrBlank()) {
                return null
            }

            return when {
                FundingType.CREDIT.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.CREDIT
                FundingType.DEBIT.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.DEBIT
                FundingType.PREPAID.equals(possibleFundingType, ignoreCase = true) ->
                    FundingType.PREPAID
                else -> FundingType.UNKNOWN
            }
        }
    }
}
