package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optDouble
import com.stripe.android.core.model.StripeJsonUtils.optLong
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
                campaign = incentive.optString("campaign"),
                incentiveParams = incentiveParams,
            )
        }
    }

    private fun buildIncentiveParams(json: JSONObject): LinkConsumerIncentive.IncentiveParams {
        val amountFlat = optLong(json, "amount_flat")
        val amountPercent = optDouble(json, "amount_percent")?.toFloat()
        val currency = optString(json, "currency")
        val paymentMethod = optString(json, "payment_method")

        return LinkConsumerIncentive.IncentiveParams(
            amountFlat = amountFlat,
            amountPercent = amountPercent,
            currency = currency,
            paymentMethod = paymentMethod.orEmpty(),
        )
    }
}
