package com.stripe.android.model.parsers

import com.stripe.android.model.SourceReceiver
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class SourceReceiverJsonParser : ModelJsonParser<SourceReceiver> {
    override fun parse(json: JSONObject): SourceReceiver {
        return SourceReceiver(
            StripeJsonUtils.optString(json, FIELD_ADDRESS),
            json.optLong(FIELD_AMOUNT_CHARGED),
            json.optLong(FIELD_AMOUNT_RECEIVED),
            json.optLong(FIELD_AMOUNT_RETURNED)
        )
    }

    private companion object {
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_AMOUNT_CHARGED = "amount_charged"
        private const val FIELD_AMOUNT_RECEIVED = "amount_received"
        private const val FIELD_AMOUNT_RETURNED = "amount_returned"
    }
}
