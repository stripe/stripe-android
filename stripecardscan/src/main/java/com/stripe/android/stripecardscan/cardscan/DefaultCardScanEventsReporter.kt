package com.stripe.android.stripecardscan.cardscan

import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultCardScanEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val cardScanConfiguration: CardScanConfiguration
) : CardScanEventsReporter {
    override fun scanStarted() {
        durationProvider.start(DurationProvider.Key.CardScan)
        fireEvent(
            eventName = "cardscan_scan_started"
        )
    }

    override fun scanSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            eventName = "cardscan_success",
            additionalParams = durationInSecondsFromStart(duration)
        )
    }

    override fun scanFailed(error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        val params = error?.let {
            mapOf("error_message" to error.safeAnalyticsMessage)
        } ?: emptyMap()
        fireEvent(
            eventName = "cardscan_failed",
            additionalParams = durationInSecondsFromStart(duration) + params
        )
    }

    override fun scanCancelled(reason: CancellationReason) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            eventName = "cardscan_cancel",
            additionalParams = durationInSecondsFromStart(duration) + mapOf(
                "cancellation_reason" to reason.analyticsReason()
            )
        )
    }

    private fun fireEvent(
        eventName: String,
        additionalParams: Map<String, Any> = emptyMap()
    ) {
        val baseParams = cardScanConfiguration.elementsSessionId?.let {
            mapOf(
                "elements_session_id" to cardScanConfiguration.elementsSessionId
            )
        } ?: emptyMap()
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = object : AnalyticsEvent {
                    override val eventName: String
                        get() = eventName
                },
                additionalParams = additionalParams + baseParams
            )
        )
    }

    private fun durationInSecondsFromStart(duration: Duration?): Map<String, Float> {
        return duration?.let {
            mapOf("duration" to it.toDouble(DurationUnit.SECONDS).toFloat())
        } ?: emptyMap()
    }

    private fun CancellationReason.analyticsReason(): String {
        return when (this) {
            CancellationReason.Back -> "back"
            CancellationReason.CameraPermissionDenied -> "camera_permission_denied"
            CancellationReason.Closed -> "closed"
            CancellationReason.UserCannotScan -> "user_cannot_scan"
        }
    }
}
