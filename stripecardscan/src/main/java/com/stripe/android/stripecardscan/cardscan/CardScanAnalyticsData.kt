package com.stripe.android.stripecardscan.cardscan

/**
 * Accumulates metrics during a card scan session for analytics reporting.
 */
internal class CardScanAnalyticsData {
    var mlKitEnabled: Boolean = false
    var totalFramesProcessed: Long = 0
    var averageFrameRateHz: Float? = null

    var panFound: Boolean = false
    var expiryFound: Boolean = false
    var highestPanAgreement: Int = 0
    var finishReason: String? = null // "ocr_agreement" or "timeout"

    var timeToFirstDetectionMs: Long? = null
    var stateResetCount: Int = 0

    fun toParamMap(): Map<String, Any> = buildMap {
        put("ml_kit_enabled", mlKitEnabled)
        put("total_frames_processed", totalFramesProcessed)
        averageFrameRateHz?.let { put("average_fps", it) }
        put("pan_found", panFound)
        put("expiry_found", expiryFound)
        put("highest_pan_agreement", highestPanAgreement)
        finishReason?.let { put("finish_reason", it) }
        timeToFirstDetectionMs?.let { put("time_to_first_detection_ms", it) }
        put("state_reset_count", stateResetCount)
    }
}
