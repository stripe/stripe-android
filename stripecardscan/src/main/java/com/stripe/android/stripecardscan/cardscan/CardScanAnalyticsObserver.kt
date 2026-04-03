package com.stripe.android.stripecardscan.cardscan

import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Observes state transitions during a card scan session and computes analytics independently
 * from the scan logic. This keeps analytics concerns decoupled from the state machine.
 */
internal class CardScanAnalyticsObserver(
    private val mlKitEnabled: Boolean,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val scanStartTime: TimeMark = timeSource.markNow()
    private var timeToFirstDetectionMs: Long? = null
    private var stateResetCount: Int = 0
    private var ocrFoundEnteredAt: TimeMark? = null
    private var lastTransitionFromState: MainLoopState? = null
    private var lastState: MainLoopState? = null

    fun onStateTransition(previousState: MainLoopState, newState: MainLoopState) {
        if (previousState is MainLoopState.Initial && newState is MainLoopState.OcrFound) {
            ocrFoundEnteredAt = timeSource.markNow()
            if (timeToFirstDetectionMs == null) {
                timeToFirstDetectionMs = scanStartTime.elapsedNow().inWholeMilliseconds
            }
        } else if (previousState is MainLoopState.OcrFound && newState is MainLoopState.Initial) {
            stateResetCount++
            ocrFoundEnteredAt = null
        }

        if (newState is MainLoopState.Finished) {
            lastTransitionFromState = previousState
        }
        lastState = newState
    }

    fun buildAnalyticsData(
        totalFramesProcessed: Long,
        averageFrameRateHz: Float?,
    ): CardScanAnalyticsData {
        val finished = lastState as? MainLoopState.Finished
        return CardScanAnalyticsData(
            mlKitEnabled = mlKitEnabled,
            totalFramesProcessed = totalFramesProcessed,
            averageFrameRateHz = averageFrameRateHz,
            panFound = finished != null,
            expiryFound = finished != null && finished.expiryMonth != null && finished.expiryYear != null,
            finishReason = deriveFinishReason(finished),
            timeToFirstDetectionMs = timeToFirstDetectionMs,
            stateResetCount = stateResetCount,
        )
    }

    private fun deriveFinishReason(finished: MainLoopState.Finished?): String? {
        if (finished == null) return null
        return when (lastTransitionFromState) {
            is MainLoopState.ExpiryWait -> {
                if (finished.expiryMonth != null && finished.expiryYear != null) {
                    CardScanAnalyticsData.FINISH_REASON_EXPIRY_FOUND
                } else {
                    CardScanAnalyticsData.FINISH_REASON_EXPIRY_WAIT_TIMEOUT
                }
            }
            is MainLoopState.OcrFound -> {
                val ocrEnteredAt = ocrFoundEnteredAt
                if (ocrEnteredAt != null && ocrEnteredAt.elapsedNow() > MainLoopState.OCR_SEARCH_DURATION) {
                    CardScanAnalyticsData.FINISH_REASON_TIMEOUT
                } else {
                    CardScanAnalyticsData.FINISH_REASON_OCR_AGREEMENT
                }
            }
            else -> null
        }
    }
}
