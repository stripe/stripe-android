package com.stripe.android.identity.states

import android.graphics.Bitmap

/**
 * Tracks and selects the best quality frame during document scanning based on blur and confidence scores.
 *
 * The time window is anchored to when the first best frame is found (i.e., the first accepted frame).
 * This avoids behavior differences across devices with different frame rates.
 */
internal class BestFrameDetector(
    private val windowDurationMs: Long = DEFAULT_WINDOW_DURATION_MS
) {
    private var bestBitmap: Bitmap? = null
    private var bestScore: Float = Float.MIN_VALUE
    private var windowStartTimestampMs: Long = 0

    fun hasBestFrame(): Boolean = bestBitmap != null

    fun hasWindowStarted(): Boolean = windowStartTimestampMs != 0L

    fun isWindowExpired(nowTimestampMs: Long): Boolean {
        // Window is inclusive of the end timestamp; treat it as expired only after it has passed.
        return hasWindowStarted() && nowTimestampMs > (windowStartTimestampMs + windowDurationMs)
    }

    /**
     * Evaluates a frame and keeps it if it's better than the current best within the time window.
     *
     * @param bitmap The cropped image bitmap
     * @param blurScore Blur quality score (higher = less blurry)
     * @param confidenceScore ML model confidence score
     * @param timestamp Monotonic timestamp in milliseconds (e.g. from SystemClock.elapsedRealtime())
     */
    fun addFrame(
        bitmap: Bitmap,
        blurScore: Float,
        confidenceScore: Float,
        timestamp: Long
    ): Boolean {
        // Window starts when the first accepted frame is added.
        if (windowStartTimestampMs == 0L) {
            windowStartTimestampMs = timestamp
        }

        val withinWindow = timestamp <= (windowStartTimestampMs + windowDurationMs)
        if (!withinWindow) {
            // Outside the fixed window - keep the best frame we already found.
            return false
        }

        // Calculate composite quality score (blur weighted more heavily)
        val score = (BLUR_WEIGHT * blurScore) + (CONFIDENCE_WEIGHT * confidenceScore)

        if (bestBitmap == null || score > bestScore) {
            bestBitmap = bitmap
            bestScore = score
            return true
        }

        return false
    }

    /**
     * Returns the best frame's bitmap, or null if no frames were captured.
     */
    fun getBestFrameBitmap(): Bitmap? {
        return bestBitmap
    }

    /**
     * Resets the detector state.
     */
    fun reset() {
        bestBitmap = null
        bestScore = Float.MIN_VALUE
        windowStartTimestampMs = 0
    }

    companion object {
        // Weights for quality score (must sum to 1.0)
        private const val BLUR_WEIGHT = 0.6f
        private const val CONFIDENCE_WEIGHT = 0.4f

        // Fixed time window (in milliseconds) starting at the first accepted frame.
        // Frames outside this window won't replace the best frame.
        private const val DEFAULT_WINDOW_DURATION_MS = 1000L // 1 second
    }
}
