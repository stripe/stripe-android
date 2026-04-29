package com.stripe.android.lpm

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider

internal class MpeSyntheticsEventReporter(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
) {
    fun onStart() {
        durationProvider.start(DurationProvider.Key.MpeSynthetics)
    }

    fun onLoad(testName: String) {
        val duration = durationProvider.end(DurationProvider.Key.MpeSynthetics)

        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = MpeSyntheticLatencyAnalyticsEvent,
                additionalParams = mapOf(
                    "test" to testName,
                ) + duration?.let {
                    mapOf("duration" to it.inWholeMilliseconds)
                }.orEmpty()
            )
        )
    }

    private data object MpeSyntheticLatencyAnalyticsEvent : AnalyticsEvent {
        override val eventName: String = "mpe.synthetic_latency"
    }
}
