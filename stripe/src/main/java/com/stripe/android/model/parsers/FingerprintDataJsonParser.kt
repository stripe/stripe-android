package com.stripe.android.model.parsers

import com.stripe.android.FingerprintData
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class FingerprintDataJsonParser(
    private val timestampSupplier: () -> Long
) : ModelJsonParser<FingerprintData> {
    override fun parse(json: JSONObject): FingerprintData? {
        val guid = optString(json, FIELD_GUID) ?: return null
        val muid = optString(json, FIELD_MUID) ?: return null
        val sid = optString(json, FIELD_SID) ?: return null

        return FingerprintData(
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
