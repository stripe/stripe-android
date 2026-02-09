package com.stripe.android.identity.states

import android.graphics.Bitmap
import android.util.Log

/**
 * Tracks and selects the best quality frame during document scanning based on blur and confidence scores.
 * Uses a time-based sliding window to ensure consistent behavior across devices with different frame rates.
 */
internal class BestFrameDetector(
    private val windowDurationMs: Long = DEFAULT_WINDOW_DURATION_MS
) {
    private var bestBitmap: Bitmap? = null
    private var bestScore: Float = Float.MIN_VALUE
    private var bestFrameTimestamp: Long = 0
    private var frameCount = 0
    private var startTime: Long = System.currentTimeMillis()

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
        
        // Calculate composite quality score (blur weighted more heavily)
        val score = (BLUR_WEIGHT * blurScore) + (CONFIDENCE_WEIGHT * confidenceScore)
        val elapsed = timestamp - startTime


        // Only consider frames within the time window
        if (timestamp - bestFrameTimestamp <= windowDurationMs && score > bestScore) {
            bestBitmap = bitmap
            bestScore = score
            bestFrameTimestamp = timestamp
        } else if (timestamp - bestFrameTimestamp > windowDurationMs) {
            // Window expired, this becomes the new best by default
            bestBitmap = bitmap
            bestScore = score
            bestFrameTimestamp = timestamp
        }
    }

    /**
     * Returns the best frame's bitmap, or null if no frames were captured.
     */
    fun getBestFrameBitmap(): Bitmap? {
        if (bestBitmap != null) {
        return bestBitmap
    }

    /**
     * Resets the detector state.
     */
    fun reset() {
        bestBitmap = null
        bestScore = Float.MIN_VALUE
        bestFrameTimestamp = 0
        frameCount = 0
        startTime = System.currentTimeMillis()
    }

    companion object {
        // Weights for quality score (must sum to 1.0)
        private const val BLUR_WEIGHT = 0.6f
        private const val CONFIDENCE_WEIGHT = 0.4f
        
        // Time window to keep looking for better frames (in milliseconds)
        // After this duration from the current best frame, any new frame becomes the new best
        private const val DEFAULT_WINDOW_DURATION_MS = 1000L // 1 second
    }
}
