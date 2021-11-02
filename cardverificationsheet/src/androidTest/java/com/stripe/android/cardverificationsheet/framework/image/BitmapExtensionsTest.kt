package com.stripe.android.cardverificationsheet.framework.image

import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.cardverificationsheet.test.R
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BitmapExtensionsTest {

    private val testResources = InstrumentationRegistry.getInstrumentation().context.resources

    @Test
    @SmallTest
    fun bitmap_scale_isCorrect() {
        // read in a sample bitmap file
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
        assertNotNull(bitmap)
        assertEquals(600, bitmap.width, "Bitmap width is not expected")
        assertEquals(375, bitmap.height, "Bitmap height is not expected")

        // scale the bitmap
        val scaledBitmap = bitmap.scale(0.2F)

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
        val bitmap = testResources.getDrawable(R.drawable.ocr_card_numbers, null)
            .toBitmap()
        assertNotNull(bitmap)
        assertEquals(600, bitmap.width, "Bitmap width is not expected")
        assertEquals(375, bitmap.height, "Bitmap height is not expected")

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
                bitmap.height * 3 / 4 - bitmap.height / 4,
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
}
