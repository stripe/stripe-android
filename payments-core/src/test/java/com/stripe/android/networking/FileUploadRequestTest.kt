package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FileFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeFilePurpose
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FileUploadRequestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileFactory = FileFactory(context)

    @Test
    fun writeBody_shouldWriteExpectedNumberOfBytes() {
        val request = FileUploadRequest(
            StripeFileParams(
                file = fileFactory.create(),
                purpose = StripeFilePurpose.IdentityDocument
            ),
            OPTIONS,
            boundary = "5955816017232305695"
        )

        ByteArrayOutputStream().use {
            request.writePostBody(it)

            assertThat(it.size()).isEqualTo(1247)
        }
    }

    @Test
    fun purposeContents_shouldReturnExpectedValue() {
        val request = FileUploadRequest(
            StripeFileParams(
                file = fileFactory.create(),
                purpose = StripeFilePurpose.IdentityDocument
            ),
            OPTIONS,
            boundary = "5955816017232305695"
        )

        val expected =
            """
            --5955816017232305695
            Content-Disposition: form-data; name="purpose"
    
            identity_document
            
            """.trimIndent()
        assertThat(request.purposeContents).isEqualTo(expected)
    }

    @Test
    fun fileMetadata_shouldReturnExpectedValue() {
        val request = FileUploadRequest(
            StripeFileParams(
                file = fileFactory.create(),
                purpose = StripeFilePurpose.IdentityDocument
            ),
            OPTIONS,
            boundary = "5955816017232305695"
        )

        val expected =
            """
            --5955816017232305695
            Content-Disposition: form-data; name="file"; filename="example.png"
            Content-Type: image/png
            Content-Transfer-Encoding: binary
            
            
            """.trimIndent()
        assertThat(request.fileMetadata).isEqualTo(expected)
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
