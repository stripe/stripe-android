package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
