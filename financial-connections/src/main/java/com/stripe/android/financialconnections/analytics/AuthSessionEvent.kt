package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.utils.filterNotNullValues
import org.json.JSONObject
import java.util.Date

internal sealed class AuthSessionEvent(
    val name: String,
    open val timestamp: Date,
    open val rawEventDetails: Map<String, String> = emptyMap()
) {

    data class Launched(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "launched",
        timestamp = timestamp
    )

    data class OAuthLaunched(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "oauth-launched",
        timestamp = timestamp
    )

    data class Loaded(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "loaded",
        timestamp = timestamp
    )

    data class Success(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "success",
        timestamp = timestamp
    )

    data class Failure(
        override val timestamp: Date,
        val error: Throwable,
    ) : AuthSessionEvent(
        name = "failure",
        timestamp = timestamp,
        rawEventDetails = error
            .toEventParams(extraMessage = null)
            .filterNotNullValues()
    )

    data class Cancel(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "cancel",
        timestamp = timestamp
    )

    data class Retry(
        override val timestamp: Date
    ) : AuthSessionEvent(
        name = "retry",
        timestamp = timestamp
    )

    fun toMap(): Map<String, Any> = mapOf(
        "event_namespace" to "partner-auth-lifecycle",
        "event_name" to name,
        "client_timestamp" to timestamp.time.toString(),
        "raw_event_details" to JSONObject(rawEventDetails).toString()
    )
}
