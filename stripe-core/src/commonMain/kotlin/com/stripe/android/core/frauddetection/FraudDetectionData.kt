package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CommonParcelize
import com.stripe.android.core.model.StripeModel
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
@CommonParcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    fun isExpired(currentTime: Long): Boolean {
        return (currentTime - timestamp) > TTL
    }

    internal companion object {
        private const val KEY_GUID = "guid"
        private const val KEY_MUID = "muid"
        private const val KEY_SID = "sid"

        private val TTL = 30.minutes.inWholeMilliseconds
    }
}
