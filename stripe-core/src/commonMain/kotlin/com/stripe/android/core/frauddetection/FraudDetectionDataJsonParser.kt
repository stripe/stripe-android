package com.stripe.android.core.frauddetection

import com.stripe.android.core.model.parsers.StripeModelParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class FraudDetectionDataJsonParser(
    private val timestampSupplier: () -> Long,
    private val json: Json = Json
) : StripeModelParser<FraudDetectionData> {
    override fun parse(jsonString: String): FraudDetectionData? {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val guid = jsonObject.string(FIELD_GUID) ?: return null
        val muid = jsonObject.string(FIELD_MUID) ?: return null
        val sid = jsonObject.string(FIELD_SID) ?: return null

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

private fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}
