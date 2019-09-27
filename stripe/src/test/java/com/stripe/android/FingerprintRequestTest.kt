package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestTest {
    @Test
    fun getContentType() {
        assertEquals(
            "application/json; charset=UTF-8",
            FingerprintRequest(emptyMap(), "guid").contentType
        )
    }

    @Test
    fun getHeaders() {
        val headers = FingerprintRequest(emptyMap(), "guid").headers
        assertEquals(
            StripeRequest.DEFAULT_USER_AGENT,
            headers[StripeRequest.HEADER_USER_AGENT]
        )
        assertEquals("m=guid", headers["Cookie"])
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun getOutputBytes() {
        val telemetryClientUtil =
            TelemetryClientUtil(ApplicationProvider.getApplicationContext<Context>())
        val output =
            FingerprintRequest(telemetryClientUtil.createTelemetryMap(), "guid")
                .getOutputBytes()
        assertTrue(output.isNotEmpty())
    }
}
