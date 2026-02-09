package com.stripe.android.identity.states

import android.graphics.Bitmap
import android.util.Log

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
    private var bestFrameTimestampMs: Long = 0
    private var windowStartTimestampMs: Long = 0
    private var lastFrameTimestampMs: Long = 0
    private var frameCount = 0

    /**
     * Evaluates a frame and keeps it if it's better than the current best within the time window.
     * 
     * @param bitmap The cropped image bitmap
     * @param blurScore Blur quality score (higher = less blurry)
     * @param confidenceScore ML model confidence score
     * @param timestamp Timestamp in milliseconds
     */
    fun addFrame(
        bitmap: Bitmap,
        blurScore: Float,
        confidenceScore: Float,
        timestamp: Long
    ) {
        frameCount++
        lastFrameTimestampMs = timestamp

        // Window starts when the first best frame is found (first accepted frame).
        if (windowStartTimestampMs == 0L) {
            windowStartTimestampMs = timestamp
        }

        val elapsedMs = timestamp - windowStartTimestampMs
        val windowEndTimestampMs = windowStartTimestampMs + windowDurationMs
        val withinWindow = timestamp <= windowEndTimestampMs

        // Calculate composite quality score (blur weighted more heavily)
        val score = (BLUR_WEIGHT * blurScore) + (CONFIDENCE_WEIGHT * confidenceScore)


        if (!withinWindow) {
            // Outside the fixed window - keep the best frame we already found.
            return
        }

        if (bestBitmap == null || score > bestScore) {
            bestBitmap = bitmap
            bestScore = score
            bestFrameTimestampMs = timestamp
        }
    }

    /**
     * Returns the best frame's bitmap, or null if no frames were captured.
     */
    fun getBestFrameBitmap(): Bitmap? {
        if (bestBitmap != null) {
            val durationMs = if (windowStartTimestampMs > 0L) {
                lastFrameTimestampMs - windowStartTimestampMs
            } else {
                0L
            }
            val bestAtMs = if (bestFrameTimestampMs > 0L && windowStartTimestampMs > 0L) {
                bestFrameTimestampMs - windowStartTimestampMs
            } else {
                0L
            }
            )
        return bestBitmap
    }

    /**
     * Resets the detector state.
     */
    fun reset() {
        val durationMs = if (windowStartTimestampMs > 0L) {
            lastFrameTimestampMs - windowStartTimestampMs
        } else {
            0L
        }
        val bestAtMs = if (bestFrameTimestampMs > 0L && windowStartTimestampMs > 0L) {
            bestFrameTimestampMs - windowStartTimestampMs
        } else {
            0L
        }
        bestBitmap = null
        bestScore = Float.MIN_VALUE
        bestFrameTimestampMs = 0
        windowStartTimestampMs = 0
        lastFrameTimestampMs = 0
        frameCount = 0
    }

    companion object {
        private const val TAG = "BestFrameDetector"
        
        // Weights for quality score (must sum to 1.0)
        private const val BLUR_WEIGHT = 0.6f
        private const val CONFIDENCE_WEIGHT = 0.4f
        
        // Fixed time window (in milliseconds) starting at the first accepted frame.
        // Frames outside this window won't replace the best frame.
        private const val DEFAULT_WINDOW_DURATION_MS = 1000L // 1 second
    }
}
