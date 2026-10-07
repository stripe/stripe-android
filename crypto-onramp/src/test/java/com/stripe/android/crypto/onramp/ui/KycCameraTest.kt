package com.stripe.android.crypto.onramp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class KycCameraTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `camera prefers jpeg and normalizes extensions`() {
        assertThat(kycCameraFormat(listOf("PNG", ".JPEG"))).isEqualTo("jpg")
        assertThat(kycCameraFormat(listOf("jpg"))).isEqualTo("jpg")
    }

    @Test
    fun `camera supports png only requirements`() {
        assertThat(kycCameraFormat(listOf("png"))).isEqualTo("png")
    }

    @Test
    fun `camera is unavailable for non image formats`() {
        assertThat(kycCameraFormat(listOf("pdf", "docx"))).isNull()
    }

    @Test
    fun `png conversion rotates camera pixels and creates a real png`() {
        val source = image()
        ExifInterface(source.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val result = prepareKycCameraImage(source, "png", 100_000)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(result.path, bounds)
        assertThat(bounds.outMimeType).isEqualTo("image/png")
        assertThat(bounds.outWidth).isEqualTo(10)
        assertThat(bounds.outHeight).isEqualTo(20)
    }

    @Test
    fun `oversized png is rejected without leaking converted file`() {
        val source = image()
        val result = runCatching { prepareKycCameraImage(source, "png", 1) }
        assertThat(result.exceptionOrNull()).isInstanceOf(KycFileTooLargeException::class.java)
        assertThat(folder.root.listFiles()?.toList()).containsExactly(source)
    }

    @Test
    fun `jpeg capture produces an uploadable jpeg within the size limit`() {
        val result = prepareKycCameraImage(image(), "jpg", 100_000)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(result.path, bounds)
        assertThat(bounds.outMimeType).isEqualTo("image/jpeg")
        assertThat(result.length()).isAtMost(100_000L)
        assertThat(result.length()).isGreaterThan(0L)
    }

    @Test
    fun `unreadable capture does not leave a converted file`() {
        val source = folder.newFile("invalid.jpg")
        val result = runCatching { prepareKycCameraImage(source, "jpg", 100_000) }
        assertThat(result.isFailure).isTrue()
        assertThat(folder.root.listFiles()?.toList()).containsExactly(source)
    }

    @Test
    fun `image within server size limit keeps dimensions larger than 4096 pixels`() {
        val source = folder.newFile("wide.jpg")
        val bitmap = Bitmap.createBitmap(5_000, 4, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()

        val result = prepareKycCameraImage(source, "jpg", 1_000_000)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(result.path, bounds)
        assertThat(bounds.outWidth).isEqualTo(5_000)
        assertThat(bounds.outHeight).isEqualTo(4)
    }

    private fun image(): File {
        val source = folder.newFile("capture.jpg")
        val bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return source
    }
}
