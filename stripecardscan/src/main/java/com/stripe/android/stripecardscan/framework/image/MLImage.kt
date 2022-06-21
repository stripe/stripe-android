package com.stripe.android.stripecardscan.framework.image

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.roundToInt

private const val DIM_PIXEL_SIZE = 3
private const val NUM_BYTES_PER_CHANNEL = 4 // Float.size / Byte.size

internal data class ImageTransformValues(val red: Float, val green: Float, val blue: Float)

/**
 * An image in the required ML input format (array of floats, 3 floats per pixel in R, G, B format).
 */
internal class MLImage(val width: Int, val height: Int, private val imageData: ByteBuffer) {

    constructor(bitmap: Bitmap, mean: Float = 0F, std: Float = 255F) : this(
        bitmap,
        ImageTransformValues(mean, mean, mean),
        ImageTransformValues(std, std, std)
    )

    constructor(bitmap: Bitmap, mean: ImageTransformValues, std: ImageTransformValues) : this(
        bitmap.width,
        bitmap.height,
        IntArray(bitmap.width * bitmap.height)
            .also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
            .let {
                val rgbFloat = ByteBuffer.allocateDirect(
                    bitmap.width * bitmap.height * DIM_PIXEL_SIZE * NUM_BYTES_PER_CHANNEL
                )
                rgbFloat.order(ByteOrder.nativeOrder())

                it.forEach {
                    // ignore the alpha value ((it shr 24 and 0xFF) - mean.alpha) / std.alpha)
                    rgbFloat.putFloat(((it shr 16 and 0xFF) - mean.red) / std.red)
                    rgbFloat.putFloat(((it shr 8 and 0xFF) - mean.green) / std.green)
                    rgbFloat.putFloat(((it and 0xFF) - mean.blue) / std.blue)
                }

                rgbFloat
            }
    )

    /**
     * Convert an [MLImage] to a [Bitmap]. This is primarily used in testing.
     */
    fun toBitmap(mean: Float = 0F, std: Float = 255F) = toBitmap(
        ImageTransformValues(mean, mean, mean),
        ImageTransformValues(std, std, std)
    )

    /**
     * Convert an [MLImage] to a [Bitmap]. This is primarily used in testing.
     */
    fun toBitmap(mean: ImageTransformValues, std: ImageTransformValues): Bitmap {
        imageData.rewind()
        check(imageData.limit() == width * height) {
            "ByteBuffer limit does not match expected size"
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val rgba = IntBuffer.allocate(width * height)
        while (this.imageData.hasRemaining()) {
            rgba.put(
                (0xFF shl 24) + // set 0xFF for the alpha value
                    (((this.imageData.float * std.red) + mean.red).roundToInt() shl 16) +
                    (((this.imageData.float * std.green) + mean.green).roundToInt() shl 8) +
                    (((this.imageData.float * std.blue) + mean.blue).roundToInt())
            )
        }
        rgba.rewind()
        bitmap.copyPixelsFromBuffer(rgba)
        return bitmap
    }

    /**
     * Get the RBG direct [ByteBuffer] for use in ML models.
     */
    fun getData(): ByteBuffer = imageData.rewind() as ByteBuffer
}
