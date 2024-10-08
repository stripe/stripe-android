package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.SharePaymentDetails
import org.json.JSONObject

internal object SharePaymentDetailsJsonParser : ModelJsonParser<SharePaymentDetails> {

    override fun parse(json: JSONObject): SharePaymentDetails? {
        val paymentMethodId = optString(json, "payment_method") ?: return null

        return SharePaymentDetails(
            paymentMethodId = paymentMethodId,
        )
    }
}
