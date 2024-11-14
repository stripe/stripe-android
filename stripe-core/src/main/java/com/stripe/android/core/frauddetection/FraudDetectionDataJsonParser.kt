package com.stripe.android.core.frauddetection

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

internal class FraudDetectionDataJsonParser(
    private val timestampSupplier: () -> Long
) : ModelJsonParser<FraudDetectionData> {
    override fun parse(json: JSONObject): FraudDetectionData? {
        val guid = optString(json, FIELD_GUID) ?: return null
        val muid = optString(json, FIELD_MUID) ?: return null
        val sid = optString(json, FIELD_SID) ?: return null

        return FraudDetectionData(
            guid = guid,
            muid = muid,
            sid = sid,
            timestamp = timestampSupplier()
        )
    }

    private companion object {
        private const val FIELD_GUID = "guid"
        private const val FIELD_MUID = "muid"
        private const val FIELD_SID = "sid"
    }
}
