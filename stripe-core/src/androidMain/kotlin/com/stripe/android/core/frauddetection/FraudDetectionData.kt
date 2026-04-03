package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class FraudDetectionData(
    val guid: String,
    val muid: String,
    val sid: String,
    val timestamp: Long = 0L
) : StripeModel {
    val params: Map<String, String>
        get() = mapOf(
            KEY_GUID to guid,
            KEY_MUID to muid,
            KEY_SID to sid
        )

    fun toJson(): JSONObject {
        return JSONObject()
            .put(KEY_GUID, guid)
            .put(KEY_MUID, muid)
            .put(KEY_SID, sid)
            .put(KEY_TIMESTAMP, timestamp)
    }

    fun isExpired(currentTime: Long): Boolean {
        return (currentTime - timestamp) > TTL
    }

    internal companion object {
        private const val KEY_GUID = "guid"
        private const val KEY_MUID = "muid"
        private const val KEY_SID = "sid"
        internal const val KEY_TIMESTAMP = "timestamp"

        private val TTL = TimeUnit.MINUTES.toMillis(30L)
    }
}
