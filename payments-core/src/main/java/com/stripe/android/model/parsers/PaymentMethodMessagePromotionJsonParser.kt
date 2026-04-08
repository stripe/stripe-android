package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodMessagePromotionList
import org.json.JSONObject

internal class PaymentMethodMessagePromotionJsonParser : ModelJsonParser<PaymentMethodMessagePromotionList> {
    override fun parse(json: JSONObject): PaymentMethodMessagePromotionList {
        val promotions = mutableListOf<PaymentMethodMessagePromotion>()
        val paymentPlanGroups = json.optJSONArray("payment_plan_groups")
        if (paymentPlanGroups != null) {
            for (i in 0 until paymentPlanGroups.length()) {
                val group = paymentPlanGroups.get(i) as? JSONObject
                val content = group?.optJSONObject(PaymentMethodMessageJsonParser.FIELD_CONTENT) ?: continue
                val promotion = content.optJSONObject(PaymentMethodMessageJsonParser.FIELD_PROMOTION)
                val promotionMessage = promotion?.optString(PaymentMethodMessageJsonParser.FIELD_MESSAGE).takeIf {
                    !it.isNullOrBlank()
                }
                val learnMore = getLearnMore(content)
                val type = group.optString(PaymentMethodMessageJsonParser.FIELD_TYPE).takeIf { !it.isNullOrBlank() }
                if (promotionMessage != null && learnMore != null && type != null) {
                    promotions.add(
                        PaymentMethodMessagePromotion(
                            message = promotionMessage,
                            learnMore = learnMore,
                            paymentMethodType = type
                        )
                    )
                }
            }
        }
        return PaymentMethodMessagePromotionList(promotions)
    }

    private fun getLearnMore(json: JSONObject): PaymentMethodMessageLearnMore? {
        val learnMore = json.optJSONObject(PaymentMethodMessageJsonParser.FIELD_LEARN_MORE) ?: return null
        val url = learnMore.optString(PaymentMethodMessageJsonParser.FIELD_URL)
        val message = learnMore.optString(PaymentMethodMessageJsonParser.FIELD_MESSAGE)
        if (url.isNullOrBlank()) return null
        return PaymentMethodMessageLearnMore(
            url = url,
            message = message
        )
    }
}
