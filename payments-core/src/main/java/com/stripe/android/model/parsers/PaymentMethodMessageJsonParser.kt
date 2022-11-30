package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodMessage
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage? {
        val displayHtml = StripeJsonUtils.optString(json, FIELD_L_HTML)
        val learnMoreUrl = StripeJsonUtils.optString(json, FIELD_LEARN_MORE_MODAL_URL)
        return if (displayHtml != null && learnMoreUrl != null) {
            PaymentMethodMessage(
                displayHtml = displayHtml,
                learnMoreUrl = learnMoreUrl
            )
        } else {
            null
        }
    }

    private companion object {
        private const val FIELD_L_HTML = "display_l_html"
        private const val FIELD_LEARN_MORE_MODAL_URL = "learn_more_modal_url"
    }
}
