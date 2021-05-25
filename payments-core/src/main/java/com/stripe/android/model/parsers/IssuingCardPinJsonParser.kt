package com.stripe.android.model.parsers

import com.stripe.android.model.IssuingCardPin
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class IssuingCardPinJsonParser : ModelJsonParser<IssuingCardPin> {
    override fun parse(json: JSONObject): IssuingCardPin? {
        return optString(json, "pin")?.let {
            IssuingCardPin(it)
        }
    }
}
