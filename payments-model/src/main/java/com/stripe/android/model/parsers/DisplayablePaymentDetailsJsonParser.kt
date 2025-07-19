package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optLong
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.DisplayablePaymentDetails
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DisplayablePaymentDetailsJsonParser : ModelJsonParser<DisplayablePaymentDetails> {
    override fun parse(json: JSONObject): DisplayablePaymentDetails {
        return DisplayablePaymentDetails(
            defaultCardBrand = optString(json, FIELD_DEFAULT_CARD_BRAND),
            defaultPaymentType = optString(json, FIELD_DEFAULT_PAYMENT_TYPE),
            last4 = optString(json, FIELD_LAST_4),
            numberOfSavedPaymentDetails = optLong(json, FIELD_NUMBER_OF_SAVED_PAYMENT_DETAILS)
        )
    }

    private companion object {
        private const val FIELD_DEFAULT_CARD_BRAND = "default_card_brand"
        private const val FIELD_DEFAULT_PAYMENT_TYPE = "default_payment_type"
        private const val FIELD_LAST_4 = "last_4"
        private const val FIELD_NUMBER_OF_SAVED_PAYMENT_DETAILS = "number_of_saved_payment_details"
    }
}
