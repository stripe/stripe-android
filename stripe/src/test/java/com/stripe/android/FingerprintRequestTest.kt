package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestTest {
    private val fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(
        ApplicationProvider.getApplicationContext()
    )

    private val request =
        FingerprintRequest(
            params = fingerprintRequestParamsFactory.createParams(),
            guid = GUID
        )

    @Test
    fun contentType_shouldBeApplicationJson() {
        assertThat(request.contentType)
            .isEqualTo("application/json; charset=UTF-8")
    }

    @Test
    fun headers_shouldReturnExpectedMap() {
        assertThat(request.headers)
            .isEqualTo(
                mapOf(
                    "Cookie" to "m=$GUID",
                    "User-Agent" to RequestHeadersFactory.getUserAgent(),
                    "Accept-Charset" to "UTF-8"
                )
            )
    }

    @Test
    fun writeBody_shouldWriteNonEmptyBytes() {
        ByteArrayOutputStream().use {
            request.writeBody(it)
            assertThat(it.size())
                .isGreaterThan(0)
        }
    }

    private companion object {
        private val GUID = UUID.randomUUID().toString()
    }
}
