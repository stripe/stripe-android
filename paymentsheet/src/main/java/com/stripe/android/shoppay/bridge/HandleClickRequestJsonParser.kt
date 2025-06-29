package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class HandleClickRequestJsonParser @Inject constructor() : ModelJsonParser<HandleClickRequest> {

    override fun parse(json: JSONObject): HandleClickRequest? {
        val eventDataJson = json.optJSONObject(FIELD_EVENT_DATA) ?: return null
        val eventData = parseEventData(eventDataJson) ?: return null

        return HandleClickRequest(
            eventData = eventData,
        )
    }

    private fun parseEventData(json: JSONObject): HandleClickRequest.EventData? {
        val expressPaymentType = StripeJsonUtils.optString(json, FIELD_EXPRESS_PAYMENT_TYPE) ?: return null

        return HandleClickRequest.EventData(
            expressPaymentType = expressPaymentType
        )
    }

    private companion object {
        private const val FIELD_EVENT_DATA = "eventData"
        private const val FIELD_EXPRESS_PAYMENT_TYPE = "expressPaymentType"
    }
}
