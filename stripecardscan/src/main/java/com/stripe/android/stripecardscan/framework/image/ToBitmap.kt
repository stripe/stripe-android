package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.CheckResult
import java.io.ByteArrayOutputStream

/**
 * Convert a [YuvImage] to a [Bitmap]. This is not an efficient method since it uses an intermediate JPEG compression
 * and should be avoided if possible.
 */
@CheckResult
@Deprecated("This method is inefficient and should be avoided if possible")
internal fun YuvImage.toBitmap(
    crop: Rect = Rect(
        0,
        0,
        this.width,
        this.height
    ),
    quality: Int = 75
): Bitmap {
    val out = ByteArrayOutputStream()
    compressToJpeg(crop, quality, out)

    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
