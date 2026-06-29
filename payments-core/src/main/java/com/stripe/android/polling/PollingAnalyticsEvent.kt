package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsEvent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed interface PollingAnalyticsEvent : AnalyticsEvent {
    val params: Map<String, String>

    class TimedOut(
        private val paymentMethodType: String,
        private val lastKnownStatus: String?,
    ) : PollingAnalyticsEvent {
        override val params: Map<String, String>
            get() = buildMap {
                put(FIELD_PAYMENT_METHOD_TYPE, paymentMethodType)
                put(FIELD_LAST_KNOWN_STATUS, lastKnownStatus ?: "unknown")
            }
        override val eventName = "paymentsheet.polling.timed_out"
    }

    companion object {
        internal const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        internal const val FIELD_LAST_KNOWN_STATUS = "last_known_status"
    }
}
