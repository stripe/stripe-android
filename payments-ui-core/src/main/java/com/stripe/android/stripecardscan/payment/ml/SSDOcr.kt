package com.stripe.android.stripecardscan.payment.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.stripe.android.camera.framework.image.cropCameraPreviewToViewFinder
import com.stripe.android.camera.framework.image.scale

/**
 * Data types and helpers for card OCR. Previously contained the SSD TFLite
 * model; now only holds the shared [Input]/[Prediction]/[ExpiryDate] types
 * and the camera-preview-to-input conversion used by [MLKitTextRecognizer].
 */
internal object SSDOcr {

    val TRAINED_IMAGE_SIZE = Size(600, 375)

    data class Input(val cardBitmap: Bitmap) {

        /**
         * Force a generic toString method to prevent leaking information about this class'
         * parameters after R8. Without this method, this `data class` will automatically generate a
         * toString which retains the original names of the parameters even after obfuscation.
         */
        override fun toString(): String {
            return "Input"
        }
    }

    data class ExpiryDate(val month: Int, val year: Int) {

        /**
         * Force a generic toString method to prevent leaking information about this class'
         * parameters after R8. Without this method, this `data class` will automatically generate a
         * toString which retains the original names of the parameters even after obfuscation.
         */
        override fun toString(): String {
            return "ExpiryDate"
        }
    }

    data class Prediction(val pan: String?, val expiry: ExpiryDate? = null) {

        /**
         * Force a generic toString method to prevent leaking information about this class'
         * parameters after R8. Without this method, this `data class` will automatically generate a
         * toString which retains the original names of the parameters even after obfuscation.
         */
        override fun toString(): String {
            return "Prediction"
        }
    }

    /**
     * Convert a camera preview image into an [Input] for OCR analysis.
     */
    fun cameraPreviewToInput(
        cameraPreviewImage: Bitmap,
        previewBounds: Rect,
        cardFinder: Rect
    ): Input {
        val cardBitmap = cropCameraPreviewToViewFinder(cameraPreviewImage, previewBounds, cardFinder)
            .scale(TRAINED_IMAGE_SIZE)
        return Input(cardBitmap = cardBitmap)
    }
}
