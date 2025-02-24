package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.LinkConsumerIncentive
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LinkConsumerIncentiveJsonParser : ModelJsonParser<LinkConsumerIncentive> {

    override fun parse(json: JSONObject): LinkConsumerIncentive {
        val incentiveParams = buildIncentiveParams(
            json = json.getJSONObject("incentive_params"),
        )

        return LinkConsumerIncentive(
            incentiveParams = incentiveParams,
            incentiveDisplayText = optString(json, "incentive_display_text"),
        )
    }

    private fun buildIncentiveParams(json: JSONObject): LinkConsumerIncentive.IncentiveParams {
        return LinkConsumerIncentive.IncentiveParams(
            paymentMethod = json.optString("payment_method"),
        )
    }
}
