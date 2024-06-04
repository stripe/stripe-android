package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.CheckResult
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.crop
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.image.toWebP
import com.stripe.android.camera.framework.util.scaleAndCenterWithin
import com.stripe.android.stripecardscan.framework.util.ImageFormat
import com.stripe.android.stripecardscan.framework.util.ImageSettings

/**
 * Convert a [Bitmap] to an [MLImage] for use in ML models.
 */
@CheckResult
internal fun Bitmap.toMLImage(mean: Float = 0F, std: Float = 255F) = MLImage(this, mean, std)

/**
 * Convert a [Bitmap] to an [MLImage] for use in ML models.
 */
@CheckResult
internal fun Bitmap.toMLImage(mean: ImageTransformValues, std: ImageTransformValues) =
    MLImage(this, mean, std)

internal fun Bitmap.toImageFormat(
    format: ImageFormat,
    imageSettings: ImageSettings
): Pair<ByteArray, Rect> {
    // Size and crop the image per the settings.
    val maxImageSize = imageSettings.imageSize

    val cropRect = maxImageSize
        .scaleAndCenterWithin(this.size())

    val croppedImage = this
        .crop(cropRect)
        .constrainToSize(maxImageSize)

    // Now convert formats with the compression ratio from settings.
    val compressionRatio = imageSettings.compressionRatio

    // Convert to 0..100
    val convertedRatio = compressionRatio.times(100.0).toInt()

    val result = when (format) {
        ImageFormat.WEBP -> croppedImage.toWebP(convertedRatio)
        ImageFormat.HEIC,
        ImageFormat.JPEG -> croppedImage.toJpeg(convertedRatio)
    }

    return Pair(result, cropRect)
}
