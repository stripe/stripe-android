package com.stripe.android.stripecardscan.cardscan

/**
 * Analytics data collected during a card scan session.
 */
internal data class CardScanAnalyticsData(
    val mlKitEnabled: Boolean = false,
    val totalFramesProcessed: Long = 0,
    val averageFrameRateHz: Float? = null,
    val panFound: Boolean = false,
    val expiryFound: Boolean = false,
    val finishReason: String? = null,
    val timeToFirstDetectionMs: Long? = null,
    val stateResetCount: Int = 0,
) {
    fun toParamMap(): Map<String, Any> = buildMap {
        put("ml_kit_enabled", mlKitEnabled)
        put("total_frames_processed", totalFramesProcessed)
        averageFrameRateHz?.let { put("average_fps", it) }
        put("pan_found", panFound)
        put("expiry_found", expiryFound)
        finishReason?.let { put("finish_reason", it) }
        timeToFirstDetectionMs?.let { put("time_to_first_detection_ms", it) }
        put("state_reset_count", stateResetCount)
    }

    companion object {
        const val FINISH_REASON_OCR_AGREEMENT = "ocr_agreement"
        const val FINISH_REASON_TIMEOUT = "timeout"
        const val FINISH_REASON_EXPIRY_FOUND = "expiry_found"
        const val FINISH_REASON_EXPIRY_WAIT_TIMEOUT = "expiry_wait_timeout"
    }
}
