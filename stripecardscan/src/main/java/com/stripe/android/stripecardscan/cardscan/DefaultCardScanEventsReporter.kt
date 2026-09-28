package com.stripe.android.stripecardscan.cardscan

import androidx.annotation.MainThread
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

@MainThread
internal class DefaultCardScanEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val cardScanConfiguration: CardScanConfiguration
) : CardScanEventsReporter {
    private var hasLoggedMlKitFoundPan = false
    private var hasLoggedMlKitFoundExp = false
    private var hasLoggedDarkniteFoundPan = false
    private var hasLoggedModelsDisagree = false

    override fun scanStarted() {
        clearSessionState()
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
        clearSessionState()
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
        clearSessionState()
    }

    override fun scanCancelled(reason: CancellationReason) {
        val duration = durationProvider.end(DurationProvider.Key.CardScan)
        fireEvent(
            eventName = "cardscan_cancel",
            additionalParams = durationInSecondsFromStart(duration) + mapOf(
                "cancellation_reason" to reason.analyticsReason()
            )
        )
        clearSessionState()
    }

    override fun scanMlKitFoundPan() {
        fireMilestoneEvent(
            shouldLog = !hasLoggedMlKitFoundPan,
            eventName = "cardscan_mlkit_found_pan",
        ) {
            hasLoggedMlKitFoundPan = true
        }
    }

    override fun scanMlKitFoundExp() {
        fireMilestoneEvent(
            shouldLog = !hasLoggedMlKitFoundExp,
            eventName = "cardscan_mlkit_found_exp",
        ) {
            hasLoggedMlKitFoundExp = true
        }
    }

    override fun scanDarkniteFoundPan() {
        fireMilestoneEvent(
            shouldLog = !hasLoggedDarkniteFoundPan,
            eventName = "cardscan_darknite_found_pan",
        ) {
            hasLoggedDarkniteFoundPan = true
        }
    }

    override fun scanModelsDisagree() {
        fireMilestoneEvent(
            shouldLog = !hasLoggedModelsDisagree,
            eventName = "cardscan_models_disagree",
        ) {
            hasLoggedModelsDisagree = true
        }
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

    private fun fireMilestoneEvent(
        shouldLog: Boolean,
        eventName: String,
        onLogged: () -> Unit,
    ) {
        val elapsed = durationProvider.elapsed(DurationProvider.Key.CardScan)
        if (!shouldLog || elapsed == null) {
            return
        }

        onLogged()
        fireEvent(
            eventName = eventName,
            additionalParams = durationInSecondsFromStart(elapsed)
        )
    }

    private fun clearSessionState() {
        hasLoggedMlKitFoundPan = false
        hasLoggedMlKitFoundExp = false
        hasLoggedDarkniteFoundPan = false
        hasLoggedModelsDisagree = false
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
