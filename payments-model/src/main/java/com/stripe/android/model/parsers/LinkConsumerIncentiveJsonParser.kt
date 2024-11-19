package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.LinkConsumerIncentive
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LinkConsumerIncentiveJsonParser : ModelJsonParser<LinkConsumerIncentive> {

    override fun parse(json: JSONObject): LinkConsumerIncentive? {
        return json.optJSONObject("link_consumer_incentive")?.let { incentive ->
            val incentiveParams = buildIncentiveParams(
                json = incentive.getJSONObject("incentive_params"),
            )

            LinkConsumerIncentive(
                incentiveParams = incentiveParams,
                incentiveDisplayText = optString(incentive, "incentive_display_text"),
            )
        }
    }

    private fun buildIncentiveParams(json: JSONObject): LinkConsumerIncentive.IncentiveParams {
        return LinkConsumerIncentive.IncentiveParams(
            paymentMethod = json.optString("payment_method"),
        )
    }
}
