package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyFixtures
import com.stripe.android.core.FileFactory
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeFilePurpose
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FileUploadRequestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileFactory = FileFactory(context)

    @Test
    fun `upload headers preserve session flags and request idempotency`() {
        val configuration = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            .betas(setOf("upload_beta=v1"))
            .build()
        val request = FileUploadRequest(
            fileParams = StripeFileParams(fileFactory.create(), StripeFilePurpose.IdentityDocument),
            options = OPTIONS,
            apiConfiguration = configuration,
            idempotencyKey = "upload-key",
        )

        assertThat(request.headers["Stripe-Version"]).isEqualTo("2020-03-02;upload_beta=v1")
        assertThat(request.headers["Idempotency-Key"]).isEqualTo("upload-key")
    }

    @Test
    fun writeBody_shouldWriteExpectedNumberOfBytes() {
        val request = FileUploadRequest(
            StripeFileParams(
                file = fileFactory.create(),
                purpose = StripeFilePurpose.IdentityDocument
            ),
            OPTIONS,
            boundary = "5955816017232305695",
            apiConfiguration = null,
            idempotencyKey = null,
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
            boundary = "5955816017232305695",
            apiConfiguration = null,
            idempotencyKey = null,
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
            boundary = "5955816017232305695",
            apiConfiguration = null,
            idempotencyKey = null,
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
        private val OPTIONS = RequestCredentials(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, stripeAccount = null)
    }
}
