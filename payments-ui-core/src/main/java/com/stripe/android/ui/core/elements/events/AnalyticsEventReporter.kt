package com.stripe.android.ui.core.elements.events

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.uicore.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsEventReporter {
    fun onAnalyticsEvent(event: AnalyticsEvent)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalAnalyticsEventReporter =
    staticCompositionLocalOf<AnalyticsEventReporter> {
        EmptyAnalyticsEventReporter
    }

private object EmptyAnalyticsEventReporter : AnalyticsEventReporter {
    override fun onAnalyticsEvent(event: AnalyticsEvent) {
        if (BuildConfig.DEBUG) {
            error(
                "AnalyticsEventReporter.${event.eventName} was not reported"
            )
        }
    }
}