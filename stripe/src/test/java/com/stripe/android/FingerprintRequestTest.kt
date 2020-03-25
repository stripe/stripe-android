package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestTest {
    private val fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun contentType_shouldBeApplicationJson() {
        assertThat(FingerprintRequest(params = emptyMap(), guid = "guid").contentType)
            .isEqualTo("application/json; charset=UTF-8")
    }

    @Test
    fun headers_shouldReturnExpectedMap() {
        assertThat(FingerprintRequest(emptyMap(), "guid").headers)
            .isEqualTo(
                mapOf(
                    "Cookie" to "m=guid",
                    "User-Agent" to "Stripe/v1 AndroidBindings/14.2.0",
                    "Accept-Charset" to "UTF-8"
                )
            )
    }

    @Test
    fun writeBody_shouldWriteNonEmptyBytes() {
        ByteArrayOutputStream().use {
            FingerprintRequest(
                params = fingerprintRequestParamsFactory.createParams(),
                guid = "guid"
            )
                .writeBody(it)
            assertThat(it.size())
                .isGreaterThan(0)
        }
    }
}
