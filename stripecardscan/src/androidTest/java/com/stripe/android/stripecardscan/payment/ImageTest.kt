package com.stripe.android.stripecardscan.payment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Size
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.stripecardscan.framework.image.toMLImage
import com.stripe.android.stripecardscan.test.R
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageTest {

    private val testResources = InstrumentationRegistry.getInstrumentation().context.resources

    @Test
    @SmallTest
    fun bitmap_toRGBByteBuffer_isCorrect() {
        val bitmap = generateSampleBitmap()
        assertNotNull(bitmap)
        assertEquals(100, bitmap.width, "Bitmap width is not expected")
        assertEquals(100, bitmap.height, "Bitmap height is not expected")

        // convert the bitmap to a byte buffer
        val convertedImage = bitmap.toMLImage(mean = 127.5f, std = 128.5f).getData()

        // read in an expected converted file
        val rawStream = testResources.openRawResource(R.raw.sample_bitmap)
        val rawBytes = rawStream.readBytes()
        val rawImage = ByteBuffer.wrap(rawBytes)
        rawStream.close()

        // check the size of the files
        assertEquals(rawImage.limit(), convertedImage.limit(), "File size mismatch")
        rawImage.rewind()
        convertedImage.rewind()

        // check each byte of the files
        var encounteredNonZeroByte = false
        while (convertedImage.position() < convertedImage.limit()) {
            val rawImageByte = rawImage.get()
            encounteredNonZeroByte = encounteredNonZeroByte || rawImageByte.toInt() != 0
            assertEquals(
                rawImageByte,
                convertedImage.get(),
                "Difference at byte ${rawImage.position()}"
            )
        }

        assertTrue(encounteredNonZeroByte, "Bytes were all zero")
    }

    private fun generateSampleBitmap(size: Size = Size(100, 100)): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        for (x in 0 until size.width) {
            for (y in 0 until size.height) {
                val red = 255 * x / size.width
                val green = 255 * y / size.height
                val blue = 255 * x / size.height
                paint.color = Color.rgb(red, green, blue)
                canvas.drawRect(RectF(x.toFloat(), y.toFloat(), x + 1F, y + 1F), paint)
            }
        }

        canvas.drawBitmap(bitmap, 0F, 0F, paint)

        return bitmap
    }
}
