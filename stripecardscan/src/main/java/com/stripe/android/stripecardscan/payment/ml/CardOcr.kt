package com.stripe.android.stripecardscan.payment.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.stripe.android.camera.framework.image.cropCameraPreviewToViewFinder
import com.stripe.android.camera.framework.image.scale
import com.stripe.android.stripecardscan.framework.image.MLImage
import com.stripe.android.stripecardscan.framework.image.toMLImage

/** Training images are normalized with mean 127.5 and std 128.5. */
private const val IMAGE_MEAN = 127.5f
private const val IMAGE_STD = 128.5f

/**
 * Shared input/output types and preprocessing for card OCR analyzers.
 */
internal object CardOcr {

    data class Input(val ssdOcrImage: MLImage, val cardBitmap: Bitmap)

    /**
     * A recognized card expiration date with named month and year fields.
     * [year] is always a full 4-digit value (e.g. 2028, not 28).
     */
    data class Expiry(val month: Int, val year: Int) {
        override fun toString(): String = "Expiry"
    }

    data class Prediction(
        val pan: String?,
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
    ) {

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
     * Convert a camera preview image into a card OCR input.
     */
    fun cameraPreviewToInput(
        cameraPreviewImage: Bitmap,
        previewBounds: Rect,
        cardFinder: Rect
    ): Input {
        val scaled = cropCameraPreviewToViewFinder(cameraPreviewImage, previewBounds, cardFinder)
            .scale(SSDOcr.Factory.TRAINED_IMAGE_SIZE)
        return Input(
            ssdOcrImage = scaled.toMLImage(mean = IMAGE_MEAN, std = IMAGE_STD),
            cardBitmap = scaled,
        )
    }
}
