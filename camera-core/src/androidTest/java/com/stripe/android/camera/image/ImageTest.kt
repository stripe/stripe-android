package com.stripe.android.camera.image

import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.camera.framework.image.crop
import com.stripe.android.camera.framework.image.cropWithFill
import com.stripe.android.camera.framework.image.scale
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.zoom
import com.stripe.android.camera.framework.util.centerOn
import com.stripe.android.camera.framework.util.toRect
import com.stripe.android.camera.test.R
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageTest {

    private val testResources = InstrumentationRegistry.getInstrumentation().context.resources

    @Test
    @SmallTest
    fun bitmap_scale_isCorrect() {
        // read in a sample bitmap file
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null).toBitmap()
        assertNotNull(bitmap)
        // Make sure a non-empty image is read.
        assertNotEquals(0, bitmap.width, "Bitmap width is 0")
        assertNotEquals(0, bitmap.height, "Bitmap height is 0")

        // scale the bitmap
        val scaledBitmap = bitmap.scale(Size(bitmap.width / 5, bitmap.height / 5))

        // check the expected sizes of the images
        assertEquals(
            Size(bitmap.width / 5, bitmap.height / 5),
            Size(scaledBitmap.width, scaledBitmap.height),
            "Scaled image is the wrong size"
        )

        // check each pixel of the images
        var encounteredNonZeroPixel = false
        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                encounteredNonZeroPixel =
                    encounteredNonZeroPixel || scaledBitmap.getPixel(x, y) != 0
            }
        }

        assertTrue(encounteredNonZeroPixel, "Pixels were all zero")
    }

    @Test
    @SmallTest
    fun bitmap_crop_isCorrect() {
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null).toBitmap()
        assertNotNull(bitmap)
        // Make sure a non-empty image is read.
        assertNotEquals(0, bitmap.width, "Bitmap width is 0")
        assertNotEquals(0, bitmap.height, "Bitmap height is 0")

        // crop the bitmap
        val croppedBitmap = bitmap.crop(
            Rect(
                bitmap.width / 4,
                bitmap.height / 4,
                bitmap.width * 3 / 4,
                bitmap.height * 3 / 4
            )
        )

        // check the expected sizes of the images
        assertEquals(
            Size(
                bitmap.width * 3 / 4 - bitmap.width / 4,
                bitmap.height * 3 / 4 - bitmap.height / 4
            ),
            Size(croppedBitmap.width, croppedBitmap.height),
            "Cropped image is the wrong size"
        )

        // check each pixel of the images
        var encounteredNonZeroPixel = false
        for (x in 0 until croppedBitmap.width) {
            for (y in 0 until croppedBitmap.height) {
                val croppedPixel = croppedBitmap.getPixel(x, y)
                val originalPixel =
                    bitmap.getPixel(x + bitmap.width / 4, y + bitmap.height / 4)
                assertEquals(originalPixel, croppedPixel, "Difference at pixel $x, $y")
                encounteredNonZeroPixel = encounteredNonZeroPixel || croppedPixel != 0
            }
        }

        assertTrue(encounteredNonZeroPixel, "Pixels were all zero")
    }

    @Test
    @SmallTest
    fun bitmap_cropWithFill_isCorrect() {
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null).toBitmap()
        assertNotNull(bitmap)
        // Make sure a non-empty image is read.
        assertNotEquals(0, bitmap.width, "Bitmap width is 0")
        assertNotEquals(0, bitmap.height, "Bitmap height is 0")

        val cropRegion = Rect(
            -100,
            -100,
            bitmap.width + 100,
            bitmap.height + 100
        )

        // crop the bitmap
        val croppedBitmap = bitmap.cropWithFill(
            cropRegion
        )

        // check the expected sizes of the images
        assertEquals(
            Size(
                bitmap.width + 200,
                bitmap.height + 200
            ),
            Size(croppedBitmap.width, croppedBitmap.height),
            "Cropped image is the wrong size"
        )

        for (y in 0 until croppedBitmap.height) {
            for (x in 0 until croppedBitmap.width) {
                if (
                    x < 100 ||
                    x > croppedBitmap.width - 100 ||
                    y < 100 ||
                    y > croppedBitmap.height - 100
                ) {
                    val croppedPixel = croppedBitmap.getPixel(x, y)
                    assertEquals(Color.GRAY, croppedPixel, "Pixel $x, $y not gray")
                }
            }
        }

        // check each pixel of the images
        var encounteredNonZeroPixel = false
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val croppedPixel = croppedBitmap.getPixel(x + 100, y + 100)
                val originalPixel = bitmap.getPixel(x, y)
                assertEquals(originalPixel, croppedPixel, "Difference at pixel $x, $y")
                encounteredNonZeroPixel = encounteredNonZeroPixel || croppedPixel != 0
            }
        }

        assertTrue(encounteredNonZeroPixel, "Pixels were all zero")
    }

    @Test
    @SmallTest
    fun zoom_isCorrect() {
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null).toBitmap()
        assertNotNull(bitmap)
        // Make sure a non-empty image is read.
        assertNotEquals(0, bitmap.width, "Bitmap width is 0")
        assertNotEquals(0, bitmap.height, "Bitmap height is 0")

        // zoom the bitmap
        val zoomedBitmap = bitmap.zoom(
            originalRegion = Size(224, 224).centerOn(bitmap.size().toRect()),
            newRegion = Rect(112, 112, 336, 336),
            newImageSize = Size(448, 448)
        )

        // check the expected sizes of the images
        assertEquals(
            Size(448, 448),
            Size(zoomedBitmap.width, zoomedBitmap.height),
            "Zoomed image is the wrong size"
        )

        // check each pixel of the images
        var encounteredNonZeroPixel = false
        for (x in 0 until zoomedBitmap.width) {
            for (y in 0 until zoomedBitmap.height) {
                val zoomedPixel = zoomedBitmap.getPixel(x, y)
                encounteredNonZeroPixel = encounteredNonZeroPixel || zoomedPixel != 0
            }
        }

        assertTrue(encounteredNonZeroPixel, "Pixels were all zero")
    }
}
