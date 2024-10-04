package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optBoolean
import com.stripe.android.core.model.StripeJsonUtils.optLong
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerFinancialIncentive
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerFinancialIncentiveJsonParser : ModelJsonParser<ConsumerFinancialIncentive> {

    override fun parse(json: JSONObject): ConsumerFinancialIncentive? {
        val isEligible = optBoolean(json, "eligible").takeIf { json.has("eligible") } ?: return null
        val incentiveAmount = optLong(json, "incentive_amount") ?: return null

        return ConsumerFinancialIncentive(
            isEligible = isEligible,
            amount = incentiveAmount,
        )
    }
}
