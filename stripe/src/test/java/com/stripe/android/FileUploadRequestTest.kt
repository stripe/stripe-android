package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeFilePurpose
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
            request.writeBody(it)

            assertEquals(
                1247,
                it.size()
            )
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

        val expected = """
            --5955816017232305695
            Content-Disposition: form-data; name="purpose"
    
            identity_document
            
            """.trimIndent()
        assertEquals(expected, request.purposeContents)
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

        val expected = """
            --5955816017232305695
            Content-Disposition: form-data; name="file"; filename="example.png"
            Content-Type: image/png
            Content-Transfer-Encoding: binary
            
            
            """.trimIndent()
        assertEquals(expected, request.fileMetadata)
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
