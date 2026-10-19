package com.stripe.android.crypto.onramp.ui

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KycPhotoPickerTest {
    @Test
    fun `one image format filters the picker to that mime type`() {
        val type = kycPhotoPickerMediaType(listOf("pdf", " .PNG "))

        assertThat(type).isInstanceOf(PickVisualMedia.SingleMimeType::class.java)
        assertThat((type as PickVisualMedia.SingleMimeType).mimeType).isEqualTo("image/png")
    }

    @Test
    fun `jpeg aliases use a single mime type`() {
        val type = kycPhotoPickerMediaType(listOf("JPG", ".jpeg"))

        assertThat(type).isInstanceOf(PickVisualMedia.SingleMimeType::class.java)
        assertThat((type as PickVisualMedia.SingleMimeType).mimeType).isEqualTo("image/jpeg")
    }

    @Test
    fun `multiple image formats show photos without videos`() {
        assertThat(kycPhotoPickerMediaType(listOf("jpg", "png", "pdf")))
            .isEqualTo(PickVisualMedia.ImageOnly)
    }

    @Test
    fun `document only requirements do not offer a photo picker`() {
        assertThat(kycPhotoPickerMediaType(listOf("pdf", "docx"))).isNull()
    }

    @Test
    fun `unrestricted formats allow photos`() {
        assertThat(kycPhotoPickerMediaType(emptyList())).isEqualTo(PickVisualMedia.ImageOnly)
    }
}
