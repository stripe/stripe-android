package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.RenderScript
import androidx.annotation.CheckResult
import com.stripe.android.camera.framework.exception.ImageTypeNotSupportedException
import com.stripe.android.camera.framework.image.NV21Image
import com.stripe.android.camera.framework.image.crop

/**
 * Determine if this application supports an image format.
 */
@CheckResult
internal fun Image.isSupportedFormat() = isSupportedFormat(this.format)

/**
 * Determine if this application supports an image format.
 */
@CheckResult
internal fun isSupportedFormat(imageFormat: Int) = when (imageFormat) {
    ImageFormat.YUV_420_888, ImageFormat.JPEG -> true
    ImageFormat.NV21 -> false // this fails on devices with android API 21.
    else -> false
}

/**
 * Convert an image to a bitmap for processing. This will throw an [ImageTypeNotSupportedException]
 * if the image type is not supported (see [isSupportedFormat]).
 */
@CheckResult
@Throws(ImageTypeNotSupportedException::class)
internal fun Image.toBitmap(
    renderScript: RenderScript,
    crop: Rect = Rect(
        0,
        0,
        this.width,
        this.height
    )
): Bitmap = when (this.format) {
    ImageFormat.NV21 -> NV21Image(this).crop(crop).toBitmap(renderScript)
    ImageFormat.YUV_420_888 -> NV21Image(this).crop(crop).toBitmap(renderScript)
    ImageFormat.JPEG -> jpegToBitmap().crop(crop)
    else -> throw ImageTypeNotSupportedException(this.format)
}

@CheckResult
private fun Image.jpegToBitmap(): Bitmap {
    require(format == ImageFormat.JPEG) { "Image is not in JPEG format" }

    val imageBuffer = planes[0].buffer
    val imageBytes = ByteArray(imageBuffer.remaining())
    imageBuffer.get(imageBytes)
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
