package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.cropWithFill
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.ml.BoundingBox
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class DefaultIdentityIOTest {
    @Test
    fun `createTestModeFileToUpload creates document front placeholder`() = runScenario(
        image = TestModeImage.DOCUMENT_FRONT,
        assetPath = "stripe_identity_test_mode/document_front.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_document_front.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    @Test
    fun `createTestModeFileToUpload creates document back placeholder`() = runScenario(
        image = TestModeImage.DOCUMENT_BACK,
        assetPath = "stripe_identity_test_mode/document_back.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_document_back.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    @Test
    fun `createTestModeFileToUpload creates selfie placeholder`() = runScenario(
        image = TestModeImage.SELFIE,
        assetPath = "stripe_identity_test_mode/selfie.png"
    ) {
        assertThat(file.name).isEqualTo("stripe_identity_test_mode_selfie.png")
        assertThat(file.readBytes()).isEqualTo(assetBytes)
    }

    @Test
    fun `resizing preserves aspect ratio and caller owned bitmap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888)
        try {
            val file = DefaultIdentityIO(context).resizeBitmapAndCreateFileToUpload(
                bitmap, "session", "session_front.jpeg", 50, 0.9f
            )
            assertThat(file.name).isEqualTo("session_front.jpeg")
            val decoded = BitmapFactory.decodeFile(file.path)
            try {
                assertThat(decoded.width).isEqualTo(50)
                assertThat(decoded.height).isEqualTo(30)
                assertThat(bitmap.isRecycled).isFalse()
            } finally {
                decoded.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `reading a URI creates the expected JPEG`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "source.png")
        val bitmap = Bitmap.createBitmap(60, 100, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val file = DefaultIdentityIO(context).resizeUriAndCreateFileToUpload(
            Uri.fromFile(source), "session", true, "back", 50, 0.9f
        )
        assertThat(file.name).isEqualTo("session_back_full_frame.jpeg")
        val decoded = BitmapFactory.decodeFile(file.path)
        try {
            assertThat(decoded.width).isEqualTo(30)
            assertThat(decoded.height).isEqualTo(50)
        } finally {
            decoded.recycle()
        }
    }

    @Test
    fun `unreadable image fails without leaving an output file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "invalid.png").apply { writeText("invalid image") }
        assertFailsWith<IOException> {
            DefaultIdentityIO(context).resizeUriAndCreateFileToUpload(
                Uri.fromFile(source), "invalid", false, "front", 50, 0.9f
            )
        }
        assertThat(File(context.filesDir, "invalid_front.jpeg").exists()).isFalse()
    }

    @Test
    fun `failed encoding removes its incomplete output`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        bitmap.recycle()
        assertFailsWith<IllegalStateException> {
            DefaultIdentityIO(context).resizeBitmapAndCreateFileToUpload(
                bitmap, "session", "failed.jpeg", 50, 0.9f
            )
        }
        assertThat(File(context.filesDir, "failed.jpeg").exists()).isFalse()
    }

    @Test
    fun `small images retain their size and remain owned by the caller`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(24, 12, Bitmap.Config.ARGB_8888)
        try {
            val file = DefaultIdentityIO(context).resizeBitmapAndCreateFileToUpload(
                bitmap, "session", "small.jpeg", 50, 0.9f
            )
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            assertThat(bounds.outMimeType).isEqualTo("image/jpeg")
            assertThat(bounds.outWidth).isEqualTo(24)
            assertThat(bounds.outHeight).isEqualTo(12)
            assertThat(bitmap.isRecycled).isFalse()
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `failed encoding can be retried at the same destination`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val identityIO = DefaultIdentityIO(context)
        val recycled = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply { recycle() }
        assertFailsWith<IllegalStateException> {
            identityIO.resizeBitmapAndCreateFileToUpload(recycled, "session", "retry.jpeg", 50, 0.9f)
        }
        assertThat(File(context.filesDir, "retry.jpeg").exists()).isFalse()

        val bitmap = Bitmap.createBitmap(60, 100, Bitmap.Config.ARGB_8888)
        try {
            val file = identityIO.resizeBitmapAndCreateFileToUpload(bitmap, "session", "retry.jpeg", 50, 0.9f)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            assertThat(bounds.outMimeType).isEqualTo("image/jpeg")
            assertThat(bounds.outWidth).isEqualTo(30)
            assertThat(bounds.outHeight).isEqualTo(50)
            assertThat(bitmap.isRecycled).isFalse()
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `JPEG output matches the previous encoder at high quality`() {
        assertJpegMatchesPreviousEncoder(.92f)
    }

    @Test
    fun `JPEG output matches the previous encoder at low quality`() {
        assertJpegMatchesPreviousEncoder(.82f)
    }

    @Test
    fun `landscape crop preserves pixels with fractional bounds and padding`() {
        assertCropMatchesPreviousImplementation(48, 32, BoundingBox(.13f, .17f, .41f, .37f), 2.75f)
    }

    @Test
    fun `portrait crop preserves rounding of odd source dimensions`() {
        assertCropMatchesPreviousImplementation(31, 47, BoundingBox(.13f, .17f, .41f, .37f), 2.75f)
    }

    @Test
    fun `square crop preserves rounding of odd source dimensions`() {
        assertCropMatchesPreviousImplementation(41, 41, BoundingBox(.2f, .15f, .6f, .7f), 0f)
    }

    @Test
    fun `padding outside the model square stays gray`() {
        assertCropMatchesPreviousImplementation(60, 30, BoundingBox(0f, 0f, 1f, 1f), 8f)
    }

    @Test
    fun `crop preserves transparent pixels and density behavior`() {
        assertCropMatchesPreviousImplementation(
            48, 32, BoundingBox(.1f, .1f, .8f, .8f), 5f, transparent = true, density = 320
        )
    }

    @Test
    fun `crop preserves RGB565 behavior`() {
        assertCropMatchesPreviousImplementation(
            48, 32, BoundingBox(.1f, .1f, .8f, .8f), 5f, config = Bitmap.Config.RGB_565
        )
    }

    @Test
    fun `crop without an intersection fails without recycling the source`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = patternedBitmap(48, 32)
        try {
            assertFailsWith<IllegalArgumentException> {
                DefaultIdentityIO(context).cropAndPadBitmap(source, BoundingBox(2f, 2f, .2f, .2f), 0f)
            }
            assertThat(source.isRecycled).isFalse()
        } finally {
            source.recycle()
        }
    }

    private fun assertJpegMatchesPreviousEncoder(quality: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = patternedBitmap(80, 60)
        val resized = source.constrainToSize(Size(40, 40))
        try {
            val expected = resized.toJpeg((quality * 100).toInt())
            val actual = DefaultIdentityIO(context).resizeBitmapAndCreateFileToUpload(
                source, "session", "parity.jpeg", 40, quality
            )
            assertThat(actual.readBytes()).isEqualTo(expected)
            assertThat(source.isRecycled).isFalse()
        } finally {
            if (resized !== source) resized.recycle()
            source.recycle()
        }
    }

    private fun assertCropMatchesPreviousImplementation(
        width: Int,
        height: Int,
        box: BoundingBox,
        padding: Float,
        transparent: Boolean = false,
        density: Int = Bitmap.DENSITY_NONE,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = patternedBitmap(width, height, transparent, config).apply { this.density = density }
        val originalPixels = source.pixels()
        val centered = source.cropCenter(maxAspectRatioInSize(source.size(), 1f))
        val expected = centered.cropWithFill(
            Rect(
                (centered.width * box.left - padding).toInt(),
                (centered.height * box.top - padding).toInt(),
                (centered.width * (box.left + box.width) + padding).toInt(),
                (centered.height * (box.top + box.height) + padding).toInt()
            )
        )
        try {
            val actual = DefaultIdentityIO(context).cropAndPadBitmap(source, box, padding)
            try {
                assertThat(actual.width).isEqualTo(expected.width)
                assertThat(actual.height).isEqualTo(expected.height)
                assertThat(actual.density).isEqualTo(expected.density)
                assertThat(actual.config).isEqualTo(expected.config)
                assertThat(actual.pixels()).isEqualTo(expected.pixels())
            } finally {
                actual.recycle()
            }
            assertThat(source.isRecycled).isFalse()
            assertThat(source.pixels()).isEqualTo(originalPixels)
        } finally {
            expected.recycle()
            if (centered !== source) centered.recycle()
            source.recycle()
        }
    }

    private fun patternedBitmap(
        width: Int,
        height: Int,
        transparent: Boolean = false,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): Bitmap {
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            val alpha = if (transparent) (x * 17 + y * 13) % 256 else 255
            Color.argb(alpha, (x * 31) % 256, (y * 47) % 256, (x * 13 + y * 19) % 256)
        }
        return Bitmap.createBitmap(pixels, width, height, config)
    }

    private fun Bitmap.pixels(): IntArray = IntArray(width * height).also {
        getPixels(it, 0, width, 0, 0, width, height)
    }

    private fun runScenario(
        image: TestModeImage,
        assetPath: String,
        block: Scenario.() -> Unit
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetBytes = context.assets.open(assetPath).use { it.readBytes() }
        val file = DefaultIdentityIO(context).createTestModeFileToUpload(image)

        Scenario(
            file = file,
            assetBytes = assetBytes
        ).apply(block)
    }

    private data class Scenario(
        val file: File,
        val assetBytes: ByteArray
    )
}
