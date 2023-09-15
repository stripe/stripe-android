package com.stripe.android.identity.states

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicColorMatrix
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import javax.inject.Inject

/**
 * Detector to determine if an image is blurry based on variance of the laplacian method.
 */
internal class LaplacianBlurDetector @Inject constructor(context: Context) {

    private val renderScript by lazy {
        RenderScript.create(context)
    }

    /**
     * Calculate the blur score of an image with laplacian method, return DEFAULT_SCORE if
     * error occurs.
     *
     * @param sourceBitmap original image bitmap
     *
     * @return The most luminous greyscale color value from 0x00 to 0xFF, scaled from 0 to 1.
     *         The higher the value, the more prominent the laplacian edge, the less blurry.
     */
    @Suppress("LongMethod")
    fun calculateBlurOutput(sourceBitmap: Bitmap): Float {
        try {
            // First apply a soft blur to smoothen out visual artifacts
            val smootherBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
            )
            val blurIntrinsic =
                ScriptIntrinsicBlur.create(renderScript, Element.RGBA_8888(renderScript))
            val source = Allocation.createFromBitmap(
                renderScript,
                sourceBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val blurTargetAllocation = Allocation.createFromBitmap(
                renderScript,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            blurIntrinsic.apply {
                setRadius(1f)
                setInput(source)
                forEach(blurTargetAllocation)
            }
            blurTargetAllocation.copyTo(smootherBitmap)

            // Greyscale so we're only dealing with white <--> black pixels, this is so we only need to
            // detect pixel luminosity
            val greyscaleBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
            )
            val smootherInput = Allocation.createFromBitmap(
                renderScript,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val greyscaleTargetAllocation = Allocation.createFromBitmap(
                renderScript,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )

            // Inverts and greyscales the image
            val colorIntrinsic = ScriptIntrinsicColorMatrix.create(renderScript)
            colorIntrinsic.setGreyscale()
            colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
            greyscaleTargetAllocation.copyTo(greyscaleBitmap)

            // Run edge detection algorithm using a laplacian matrix convolution
            // Apply 3x3 convolution to detect edges
            val edgesBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
            )
            val greyscaleInput = Allocation.createFromBitmap(
                renderScript,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val edgesTargetAllocation = Allocation.createFromBitmap(
                renderScript,
                edgesBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )

            val convolve =
                ScriptIntrinsicConvolve3x3.create(renderScript, Element.U8_4(renderScript))
            convolve.setInput(greyscaleInput)
            convolve.setCoefficients(CLASSIC_MATRIX) // Or use others
            convolve.forEach(edgesTargetAllocation)
            edgesTargetAllocation.copyTo(edgesBitmap)

            // Scale the score from 0~255(0xFF)
            return mostLuminousIntensityFromGreyscaleBitmap(edgesBitmap).toFloat() / COLOR_MAX
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate blur score. $e")
            return DEFAULT_SCORE
        }
    }

    /**
     * Resolves the most luminous color pixel in a given greyscale bitmap.
     *
     * @param bitmap Source greyscale bitmap, with same RGB channel value.
     * @return The most luminous color in bitmap, ranging from 0x00 to 0xFF.
     */
    private fun mostLuminousIntensityFromGreyscaleBitmap(bitmap: Bitmap): Int {
        bitmap.setHasAlpha(false)
        val pixels = IntArray(bitmap.height * bitmap.width)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var maxIntensity = -1
        for (pixel in pixels) {
            // Since this is a greyscale bitmap, its RGB channel has the same value.
            // Retrieving the Red channel for comparison.
            val intensity = Color.red(pixel)

            if (intensity > maxIntensity) {
                maxIntensity = intensity
            }
        }
        return maxIntensity
    }

    companion object {
        private const val TAG = "LaplacianBlurDetector"
        private val CLASSIC_MATRIX = floatArrayOf(
            -1.0f, -1.0f, -1.0f,
            -1.0f, 8.0f, -1.0f,
            -1.0f, -1.0f, -1.0f
        )

        private const val DEFAULT_SCORE = 1.0f
        private const val COLOR_MAX = 0xFF
    }
}
