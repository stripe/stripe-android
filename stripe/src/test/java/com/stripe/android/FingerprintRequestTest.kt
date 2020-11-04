package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networking.RequestHeadersFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestTest {
    private val fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(
        ApplicationProvider.getApplicationContext()
    )

    private val request =
        FingerprintRequest(
            params = fingerprintRequestParamsFactory.createParams(FINGERPRINT_DATA),
            guid = FINGERPRINT_DATA.guid
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
                    "Cookie" to "m=${FINGERPRINT_DATA.guid}",
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
        private val FINGERPRINT_DATA = FingerprintDataFixtures.create()
    }
}
