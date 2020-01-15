package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestTest {
    private val telemetryClientUtil: TelemetryClientUtil by lazy {
        TelemetryClientUtil(ApplicationProvider.getApplicationContext<Context>())
    }

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
            RequestHeadersFactory.DEFAULT_USER_AGENT,
            headers["User-Agent"]
        )
        assertEquals("m=guid", headers["Cookie"])
    }

    @Test
    fun writeBody_shouldWriteNonEmptyBytes() {
        FakeOutputStream().use {
            FingerprintRequest(telemetryClientUtil.createTelemetryMap(), "guid")
                .writeBody(it)
            assertTrue(it.writtenBytesSize > 0)
        }
    }
}
