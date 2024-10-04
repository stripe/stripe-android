package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optDouble
import com.stripe.android.core.model.StripeJsonUtils.optLong
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.IncentiveParams
import com.stripe.android.model.LinkConsumerIncentive
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LinkConsumerIncentiveJsonParser : ModelJsonParser<LinkConsumerIncentive> {

    override fun parse(json: JSONObject): LinkConsumerIncentive? {
        return json.optJSONObject("link_consumer_incentive")?.let { incentive ->
            LinkConsumerIncentive(
                campaign = incentive.optString("campaign"),
                paymentMethod = incentive.optString("payment_method"),
                incentiveParams = buildIncentiveParams(
                    json = incentive.getJSONObject("incentive_params"),
                ),
            )
        }
    }

    private fun buildIncentiveParams(json: JSONObject): IncentiveParams {
        val amountFlat = optLong(json, "amount_flat")
        val amountPercent = optDouble(json, "amount_percent")
        val currency = json.optString("currency")

        return if (amountFlat != null) {
            IncentiveParams.Absolute(
                amount = amountFlat,
                currency = currency,
            )
        } else {
            IncentiveParams.Relative(
                percentage = amountPercent!!.toFloat(),
                currency = currency,
            )
        }
    }
}
