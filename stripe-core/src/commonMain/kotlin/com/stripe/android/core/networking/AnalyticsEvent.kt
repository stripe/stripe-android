package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsEvent {
    /**
     * Value that will be sent as [AnalyticsFields.EVENT].
     */
    val eventName: String
}
