package com.stripe.android.crypto.onramp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.graphics.scale
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.Locale

internal fun kycCameraFormat(formats: List<String>): String? {
    val normalized = formats.map { it.trim().removePrefix(".").lowercase(Locale.ROOT) }
    return when {
        normalized.any { it == "jpg" || it == "jpeg" } -> "jpg"
        "png" in normalized -> "png"
        else -> null
    }
}

/** Camera apps write JPEG regardless of the output suffix. Re-encode and normalize orientation before upload. */
internal fun prepareKycCameraImage(source: File, format: String, maximumBytes: Long): File {
    var bitmap = BitmapFactory.decodeFile(source.path) ?: throw IOException("Unreadable camera image")
    val destination = File.createTempFile("photo-", ".$format", source.parentFile)
    var completed = false
    try {
        val matrix = cameraOrientationMatrix(
            ExifInterface(source.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        )
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) bitmap.recycle()
        bitmap = oriented
        var quality = INITIAL_JPEG_QUALITY
        while (true) {
            destination.outputStream().use { output ->
                val encoding = if (format == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                encodeCameraBitmap(bitmap, encoding, quality, output)
            }
            if (destination.length() <= maximumBytes) {
                completed = true
                return destination
            }
            if (format == "png" || minOf(bitmap.width, bitmap.height) <= 1) {
                throw AdditionalKycFileTooLargeException()
            }
            if (quality > MINIMUM_JPEG_QUALITY) {
                quality -= JPEG_QUALITY_STEP
            } else {
                val scaled = bitmap.scale(
                    (bitmap.width / 2).coerceAtLeast(1), (bitmap.height / 2).coerceAtLeast(1),
                )
                bitmap.recycle()
                bitmap = scaled
                quality = INITIAL_JPEG_QUALITY
            }
        }
    } finally {
        if (!completed) destination.delete()
        bitmap.recycle()
    }
}

private fun cameraOrientationMatrix(orientation: Int): Matrix = Matrix().apply {
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(HALF_TURN)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(QUARTER_TURN); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(QUARTER_TURN)
        ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(THREE_QUARTER_TURN); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(THREE_QUARTER_TURN)
    }
}

private fun encodeCameraBitmap(bitmap: Bitmap, encoding: Bitmap.CompressFormat, quality: Int, output: OutputStream) {
    if (!bitmap.compress(encoding, quality, output)) throw IOException("Unable to encode camera image")
}

private const val INITIAL_JPEG_QUALITY = 90
private const val MINIMUM_JPEG_QUALITY = 50
private const val JPEG_QUALITY_STEP = 20
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f
