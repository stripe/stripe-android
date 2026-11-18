package com.stripe.android.identity.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.ml.BoundingBox
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class DefaultIdentityIOCropTest {
    private val identityIO = DefaultIdentityIO(ApplicationProvider.getApplicationContext())

    @Test
    fun `portrait crop uses full resolution face dimensions in the center square`() {
        val original = patternedBitmap(width = 400, height = 600)

        val cropped = identityIO.cropAndPadBitmapAsymmetric(
            original = original,
            boundingBox = BoundingBox(0.3f, 0.35f, 0.2f, 0.25f),
            sideFraction = 0.5f,
            topFraction = 0.45f,
            bottomFraction = 0.35f
        )

        // The 80 x 100 face gets 40 pixels on each side, 45 above, and 35 below.
        // The center square starts 100 pixels below the original image's top edge.
        assertCrop(cropped, original, left = 80, top = 195, width = 160, height = 180)
    }

    @Test
    fun `landscape crop uses the horizontally centered square`() {
        val original = patternedBitmap(width = 600, height = 400)

        val cropped = identityIO.cropAndPadBitmapAsymmetric(
            original = original,
            boundingBox = BoundingBox(0.3f, 0.35f, 0.2f, 0.25f),
            sideFraction = 0.5f,
            topFraction = 0.45f,
            bottomFraction = 0.35f
        )

        assertCrop(cropped, original, left = 180, top = 95, width = 160, height = 180)
    }

    @Test
    fun `padding scales with image resolution`() {
        val original = patternedBitmap(width = 800, height = 800)

        val cropped = identityIO.cropAndPadBitmapAsymmetric(
            original = original,
            boundingBox = BoundingBox(0.3f, 0.35f, 0.2f, 0.25f),
            sideFraction = 0.5f,
            topFraction = 0.45f,
            bottomFraction = 0.35f
        )

        assertCrop(cropped, original, left = 160, top = 190, width = 320, height = 360)
    }

    @Test
    fun `zero padding crops to the face bounds`() {
        val original = patternedBitmap(width = 400, height = 600)

        val cropped = identityIO.cropAndPadBitmapAsymmetric(
            original = original,
            boundingBox = BoundingBox(0.3f, 0.35f, 0.2f, 0.25f),
            sideFraction = 0f,
            topFraction = 0f,
            bottomFraction = 0f
        )

        assertCrop(cropped, original, left = 120, top = 240, width = 80, height = 100)
    }

    @Test
    fun `padding beyond all center square edges is filled with gray`() {
        val original = patternedBitmap(width = 200, height = 400)

        val cropped = identityIO.cropAndPadBitmapAsymmetric(
            original = original,
            boundingBox = BoundingBox(0.1f, 0.1f, 0.8f, 0.8f),
            sideFraction = 0.5f,
            topFraction = 0.5f,
            bottomFraction = 0.25f
        )

        assertThat(cropped.width).isEqualTo(320)
        assertThat(cropped.height).isEqualTo(280)
        // The square occupies (60, 60) through (259, 259) in the padded output.
        assertCrop(
            Bitmap.createBitmap(cropped, 60, 60, 200, 200),
            original,
            left = 0,
            top = 100,
            width = 200,
            height = 200
        )
        assertThat(cropped.getPixel(59, 100)).isEqualTo(Color.GRAY)
        assertThat(cropped.getPixel(260, 100)).isEqualTo(Color.GRAY)
        assertThat(cropped.getPixel(100, 59)).isEqualTo(Color.GRAY)
        assertThat(cropped.getPixel(100, 260)).isEqualTo(Color.GRAY)
    }

    private fun patternedBitmap(width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            Color.rgb(x % 256, y % 256, (x / 256 + y / 256) % 256)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun assertCrop(
        actual: Bitmap,
        original: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) {
        assertThat(actual.width).isEqualTo(width)
        assertThat(actual.height).isEqualTo(height)
        assertThat(actual.sameAs(Bitmap.createBitmap(original, left, top, width, height))).isTrue()
    }
}
