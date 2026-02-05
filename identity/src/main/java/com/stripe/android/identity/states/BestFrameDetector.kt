package com.stripe.android.identity.states

import android.graphics.Bitmap
import android.util.Log

/**
 * Tracks and selects the best quality frame during document scanning based on blur and confidence scores.
 */
internal class BestFrameDetector {
    private var bestBitmap: Bitmap? = null
    private var bestScore: Float = Float.MIN_VALUE
    private var frameCount = 0

    /**
     * Evaluates a frame and keeps it if it's better than the current best.
     * 
     * @param bitmap The cropped image bitmap
     * @param blurScore Blur quality score (higher = less blurry)
     * @param confidenceScore ML model confidence score
     */
    fun addFrame(
        bitmap: Bitmap,
        blurScore: Float,
        confidenceScore: Float
    ) {
        frameCount++
        
        // Calculate composite quality score (blur weighted more heavily)
        val score = (BLUR_WEIGHT * blurScore) + (CONFIDENCE_WEIGHT * confidenceScore)


        if (score > bestScore) {
            bestBitmap = bitmap
            bestScore = score
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
        frameCount = 0
    }

    companion object {
        // Weights for quality score (must sum to 1.0)
        private const val BLUR_WEIGHT = 0.6f
        private const val CONFIDENCE_WEIGHT = 0.4f
    }
}
