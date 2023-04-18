@file:Suppress("deprecation") // ktlint-disable annotation

/*
 * RenderScript is deprecated, but alternatives are not yet well supported.
 *
 * According to the android developer blog, We should be maintaining two code paths going forward to
 * keep taking advantage of the device GPU for image processing. Native vulkan is only supported on
 * API 29+.
 *
 * https://android-developers.googleblog.com/2021/04/android-gpu-compute-going-forward.html
 *
 * More investigation is needed here to determine what the impact of using the CPU only
 * renderscript-intrinsics-replacement-toolkit vs. native vulkan is.
 *
 * https://github.com/android/renderscript-intrinsics-replacement-toolkit/blob/main/renderscript-toolkit/src/main/java/com/google/android/renderscript/Toolkit.kt
 */
package com.stripe.android.camera.framework.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.util.Log
import android.util.Size
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.exception.ImageTypeNotSupportedException
import com.stripe.android.camera.framework.util.cacheFirstResult
import com.stripe.android.camera.framework.util.mapArray
import com.stripe.android.camera.framework.util.mapToIntArray
import com.stripe.android.camera.framework.util.toByteArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv

/**
 * Get the RenderScript instance.
 */
internal val getRenderScript = cacheFirstResult { context: Context -> RenderScript.create(context) }

private val logTag: String = NV21Image::class.java.simpleName

/**
 * An image made of data in the NV21 format.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NV21Image(val width: Int, val height: Int, val nv21Data: ByteArray) {

    @Throws(ImageTypeNotSupportedException::class)
    constructor(image: Image) : this(
        image.width,
        image.height,
        when (image.format) {
            ImageFormat.NV21 -> image.planes[0].buffer.toByteArray()
            ImageFormat.YUV_420_888 -> image.yuvToNV21Bytes()
            else -> throw ImageTypeNotSupportedException(image.format)
        }
    )

    @Throws(ImageTypeNotSupportedException::class)
    constructor(yuvImage: YuvImage) : this(
        yuvImage.width,
        yuvImage.height,
        when (yuvImage.yuvFormat) {
            ImageFormat.NV21 -> yuvImage.yuvData
            else -> throw ImageTypeNotSupportedException(yuvImage.yuvFormat)
        }
    )

    /**
     * The size of the [NV21Image].
     */
    val size = Size(width, height)

    /**
     * Crop a region of the [NV21Image].
     *
     * https://www.programmersought.com/article/75461140907/
     */
    fun crop(rect: Rect) = crop(rect.left, rect.top, rect.right, rect.bottom)

    /**
     * Crop a region of the [NV21Image].
     *
     * https://www.programmersought.com/article/75461140907/
     */
    fun crop(left: Int, top: Int, right: Int, bottom: Int): NV21Image {
        if (left > width || top > height) {
            return NV21Image(0, 0, ByteArray(0))
        }

        if (left == 0 && top == 0 && right == width && bottom == height) {
            return this
        }

        // Take the couple
        val x = left * 2 / 2
        val y = top * 2 / 2
        val w = (right - left) * 2 / 2
        val h = (bottom - top) * 2 / 2

        val yUnit = w * h
        val uv = yUnit / 2
        val nData = ByteArray(yUnit + uv)
        val uvIndexDst = w * h - y / 2 * w
        val uvIndexSrc = width * height + x
        var srcPos0 = y * width
        var destPos0 = 0
        var uvSrcPos0 = uvIndexSrc
        var uvDestPos0 = uvIndexDst
        for (i in y until y + h) {
            System.arraycopy(nv21Data, srcPos0 + x, nData, destPos0, w) // y memory block copy
            srcPos0 += width
            destPos0 += w
            if ((i and 1) == 0) {
                System.arraycopy(nv21Data, uvSrcPos0, nData, uvDestPos0, w) // uv memory block copy
                uvSrcPos0 += width
                uvDestPos0 += w
            }
        }

        return NV21Image(w, h, nData)
    }

    /**
     * Rotate the NV21 image an increment of 90 degrees.
     */
    fun rotate(rotationDegrees: Int): NV21Image {
        require(rotationDegrees % 90 == 0) { "Can only rotate increments of 90 degrees" }
        val rotation =
            if (rotationDegrees % 360 < 0) rotationDegrees % 360 + 360 else rotationDegrees % 360
        if (rotation == 0) return this
        val output = ByteArray(nv21Data.size)
        val frameSize = width * height
        val swap = rotation % 180 != 0
        val xFlip = rotation % 270 != 0
        val yFlip = rotation >= 180
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIn = j * width + i
                val uIn = frameSize + (j shr 1) * width + (i and 1.inv())
                val vIn = uIn + 1
                val wOut = if (swap) height else width
                val hOut = if (swap) width else height
                val iSwapped = if (swap) j else i
                val jSwapped = if (swap) i else j
                val iOut = if (xFlip) wOut - iSwapped - 1 else iSwapped
                val jOut = if (yFlip) hOut - jSwapped - 1 else jSwapped
                val yOut = jOut * wOut + iOut
                val uOut = frameSize + (jOut shr 1) * wOut + (iOut and 1.inv())
                val vOut = uOut + 1
                output[yOut] = (0xff and nv21Data[yIn].toInt()).toByte()
                output[uOut] = (0xff and nv21Data[uIn].toInt()).toByte()
                output[vOut] = (0xff and nv21Data[vIn].toInt()).toByte()
            }
        }

        return if (rotation == 270 || rotation == 90) {
            NV21Image(height, width, output)
        } else {
            NV21Image(width, height, output)
        }
    }

//    fun scale(percent: Float) {
//        TODO("Implement this")
//    }

//    fun toRGBByteBuffer(mean: ImageTransformValues, std: ImageTransformValues) {
//        TODO("Finish implementing this")
//        val startTime = System.currentTimeMillis()
//        val frameSize = width * height
//
//        var yp = 0
//
//        val rgba = IntArray(width * height)
//
//        for (j in 0 until height) {
//            var uvp = frameSize + (j shr 1) * width
//            var u = 0
//            var v = 0
//            for (i in 0 until width) {
//                var y = (0xff and data[yp].toInt()) - 16
//                if (y < 0) y = 0
//                if (i and 1 == 0) {
//                    v = (0xff and data[uvp++].toInt()) - 128
//                    u = (0xff and data[uvp++].toInt()) - 128
//                }
//                val y1192 = 1192 * y
//                var r = y1192 + 1634 * v
//                var g = y1192 - 833 * v - 400 * u
//                var b = y1192 + 2066 * u
//                if (r < 0) r = 0 else if (r > 262143) r = 262143
//                if (g < 0) g = 0 else if (g > 262143) g = 262143
//                if (b < 0) b = 0 else if (b > 262143) b = 262143
//
//                // rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
//                // 0xff00) | ((b >> 10) & 0xff);
//                // rgba, divide 2^10 ( >> 10)
//                rgba[yp] = (r shl 14 and -0x1000000 or (g shl 6 and 0xff0000)
//                    or (b shr 2 or 0xff00))
//                yp++
//            }
//        }
//
//        val rgbFloat =
//            ByteBuffer.allocateDirect(this.width * this.height * DIM_PIXEL_SIZE * NUM_BYTES_PER_CHANNEL)
//        rgbFloat.order(ByteOrder.nativeOrder())
//
//        rgba.forEach {
//            rgbFloat.putFloat(((it shr 24 and 0xFF) - mean.red) / std.red)
//            rgbFloat.putFloat(((it shr 16 and 0xFF) - mean.green) / std.green)
//            rgbFloat.putFloat(((it shr 8 and 0xFF) - mean.blue) / std.blue)
//            // ignore the alpha value ((it and 0xFF) - mean.alpha) / std.alpha)
//        }
//
//        rgbFloat.rewind()
//        Log.d(Config.logTag, "Bitmap to RGB Byte buffer conversion took: ${System.currentTimeMillis() - startTime} ms")
//        return rgbFloat
//    }

    /**
     * Convert to a [YuvImage].
     */
    @CheckResult
    fun toYuvImage() = YuvImage(
        nv21Data,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    /**
     * https://github.com/silvaren/easyrs/blob/c8eed0f0b713bbb1eb375aca23d615677e8adb3c/easyrs/src/main/java/io/github/silvaren/easyrs/tools/YuvToRgb.java
     *
     * TODO: once the renderscript toolkit is available in maven central, replace this method with
     * the yuvToRgbBitmap from that https://github.com/android/renderscript-intrinsics-replacement-toolkit/blob/main/renderscript-toolkit/src/main/java/com/google/android/renderscript/Toolkit.kt#L1079
     */
    fun toBitmap(renderScript: RenderScript): Bitmap {
        val yuvTypeBuilder: Type.Builder =
            Type.Builder(renderScript, Element.U8(renderScript)).setX(nv21Data.size)
        val yuvType: Type = yuvTypeBuilder.create()
        val yuvAllocation = Allocation.createTyped(renderScript, yuvType, Allocation.USAGE_SCRIPT)
        yuvAllocation.copyFrom(nv21Data)

        val rgbTypeBuilder: Type.Builder =
            Type.Builder(renderScript, Element.RGBA_8888(renderScript))
        rgbTypeBuilder.setX(width)
        rgbTypeBuilder.setY(height)
        val rgbAllocation = Allocation.createTyped(renderScript, rgbTypeBuilder.create())

        val yuvToRgbScript =
            ScriptIntrinsicYuvToRGB.create(renderScript, Element.RGBA_8888(renderScript))
        yuvToRgbScript.setInput(yuvAllocation)
        yuvToRgbScript.forEach(rgbAllocation)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        rgbAllocation.copyTo(bitmap)

        // remove allocated objects
        yuvType.destroy()
        yuvAllocation.destroy()
        rgbAllocation.destroy()
        yuvToRgbScript.destroy()

        return bitmap
    }
}

/**
 * Convert YUV420_888 image into NV21
 */
@CheckResult
private fun Image.yuvToNV21Bytes() = yuvPlanesToNV21Fast(
    width = width,
    height = height,
    planeBuffers = planes.mapArray { it.buffer },
    rowStrides = planes.mapToIntArray { it.rowStride },
    pixelStrides = planes.mapToIntArray { it.pixelStride }
)

/**
 * https://stackoverflow.com/questions/32276522/convert-nv21-byte-array-into-bitmap-readable-format
 *
 * https://stackoverflow.com/questions/41773621/camera2-output-to-bitmap
 *
 * On Revvl2, average performance is ~27ms
 */
@CheckResult
private fun yuvPlanesToNV21Compat(
    width: Int,
    height: Int,
    planeBuffers: Array<ByteBuffer>,
    rowStrides: IntArray,
    pixelStrides: IntArray,
    format: Int,
    crop: Rect = Rect(0, 0, width, height)
): ByteArray {
    val cropWidth = crop.width()
    val cropHeight = crop.height()
    val nv21Bytes = ByteArray(cropWidth * cropHeight * ImageFormat.getBitsPerPixel(format) / 8)
    val rowData = ByteArray(rowStrides[0])

    var channelOffset = 0
    var outputStride = 1

    for (i in planeBuffers.indices) {
        when (i) {
            0 -> {
                channelOffset = 0
                outputStride = 1
            }
            1 -> {
                channelOffset = cropWidth * cropHeight + 1
                outputStride = 2
            }
            2 -> {
                channelOffset = cropWidth * cropHeight
                outputStride = 2
            }
        }

        val buffer = planeBuffers[i]
        val rowStride = rowStrides[i]
        val pixelStride = pixelStrides[i]
        val shift = if (i == 0) 0 else 1
        val w = cropWidth shr shift
        val h = cropHeight shr shift

        buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))

        for (row in 0 until h) {
            var length: Int

            if (pixelStride == 1 && outputStride == 1) {
                length = w
                buffer.get(nv21Bytes, channelOffset, length)
                channelOffset += length
            } else {
                length = (w - 1) * pixelStride + 1
                buffer.get(rowData, 0, length)
                for (col in 0 until w) {
                    nv21Bytes[channelOffset] = rowData[col * pixelStride]
                    channelOffset += outputStride
                }
            }

            if (row < h - 1) {
                buffer.position(buffer.position() + rowStride - length)
            }
        }
    }

    return nv21Bytes
}

/**
 * https://stackoverflow.com/questions/44994510/how-to-convert-rotate-raw-nv21-array-image-android-media-image-from-front-ca
 *
 * On Revvl2, average performance is ~60ms
 */
@CheckResult
private fun yuvPlanesToNV21Slow(planeBuffers: Array<ByteBuffer>): ByteArray {
    val rez: ByteArray
    val buffer0 = planeBuffers[0]
    val buffer1 = planeBuffers[1]
    val buffer2 = planeBuffers[2]

    // actually here should be something like each second byte
    // however I simply get the last byte of buffer 2 and the entire buffer 1
    val buffer0Size = buffer0.remaining()
    val buffer1Size = buffer1.remaining() // / 2 + 1;
    val buffer2Size = 1 // buffer2.remaining(); // / 2 + 1;
    val buffer0Byte = ByteArray(buffer0Size)
    val buffer1Byte = ByteArray(buffer1Size)
    val buffer2Byte = ByteArray(buffer2Size)
    buffer0[buffer0Byte, 0, buffer0Size]
    buffer1[buffer1Byte, 0, buffer1Size]
    buffer2[buffer2Byte, buffer2Size - 1, buffer2Size]
    val outputStream = ByteArrayOutputStream()
    try {
        // swap 1 and 2 as blue and red colors are swapped
        outputStream.write(buffer0Byte)
        outputStream.write(buffer2Byte)
        outputStream.write(buffer1Byte)
    } catch (e: IOException) {
        Log.e(logTag, "Error converting image from YUV to NV21")
    }
    rez = outputStream.toByteArray()

    return rez
}

/**
 * Utility function for converting YUV planes into an NV21 byte array
 *
 * https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776
 *
 * On Revvl2, average performance is ~5ms
 */
@CheckResult
private fun yuvPlanesToNV21Fast(
    width: Int,
    height: Int,
    planeBuffers: Array<ByteBuffer>,
    rowStrides: IntArray,
    pixelStrides: IntArray
): ByteArray {
    val ySize = width * height
    val uvSize = width * height / 4
    val nv21 = ByteArray(ySize + uvSize * 2)
    val yBuffer = planeBuffers[0] // Y
    val uBuffer = planeBuffers[1] // U
    val vBuffer = planeBuffers[2] // V
    var rowStride = rowStrides[0]
    check(pixelStrides[0] == 1)
    var pos = 0
    if (rowStride == width) { // likely
        yBuffer[nv21, 0, ySize]
        pos += ySize
    } else {
        var yBufferPos = -rowStride.toLong() // not an actual position
        while (pos < ySize) {
            yBufferPos += rowStride.toLong()
            yBuffer.position(yBufferPos.toInt())
            yBuffer[nv21, pos, width]
            pos += width
        }
    }
    rowStride = rowStrides[2]
    val pixelStride = pixelStrides[2]
    check(rowStride == rowStrides[1])
    check(pixelStride == pixelStrides[1])
    if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
        // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
        val savePixel = vBuffer[1]
        try {
            vBuffer.put(1, savePixel.inv())
            if (uBuffer[0] == savePixel.inv()) {
                vBuffer.put(1, savePixel)
                vBuffer.position(0)
                uBuffer.position(0)
                vBuffer[nv21, ySize, 1]
                uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                return nv21 // shortcut
            }
        } catch (ex: ReadOnlyBufferException) {
            // unfortunately, we cannot check if vBuffer and uBuffer overlap
        }

        // unfortunately, the check failed. We must save U and V pixel by pixel
        vBuffer.put(1, savePixel)
    }

    // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
    // but performance gain would be less significant
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val vuPos = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer[vuPos]
            nv21[pos++] = uBuffer[vuPos]
        }
    }

    return nv21
}

/**
 * https://stackoverflow.com/questions/33542708/camera2-api-convert-yuv420-to-rgb-green-out
 */
private fun yuvPlanesToBitmap(
    width: Int,
    height: Int,
    planeBuffers: Array<ByteBuffer>
): Bitmap {
    val bitmap = ByteArrayOutputStream()
    YuvImage(planeBuffers.toList().toByteArray(), ImageFormat.NV21, width, height, null)
        .compressToJpeg(Rect(0, 0, width, height), 95, bitmap)
    return BitmapFactory.decodeByteArray(bitmap.toByteArray(), 0, bitmap.size())
}
