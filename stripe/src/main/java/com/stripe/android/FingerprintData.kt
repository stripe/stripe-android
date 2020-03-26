package com.stripe.android

import java.util.concurrent.TimeUnit
import org.json.JSONObject

internal data class FingerprintData(
    internal val guid: String? = null,
    internal val timestamp: Long = 0L
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put(KEY_GUID, guid)
            .put(KEY_TIMESTAMP, timestamp)
    }

    fun isExpired(currentTime: Long): Boolean {
        return (currentTime - timestamp) > TTL
    }

    internal companion object {
        fun fromJson(json: JSONObject): FingerprintData {
            return FingerprintData(
                guid = json.optString(KEY_GUID),
                timestamp = json.optLong(
                    KEY_TIMESTAMP,
                    -1
                )
            )
        }

        private const val KEY_GUID = "guid"
        private const val KEY_TIMESTAMP = "timestamp"

        private val TTL = TimeUnit.MINUTES.toMillis(30L)
    }
}
