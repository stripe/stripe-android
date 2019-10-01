package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeFireAndForgetRequestExecutorTest {

    // Test to verify fingerprint endpoint's success
    @Test
    @Throws(InvalidRequestException::class, APIConnectionException::class)
    fun execute_withFingerprintRequest_shouldReturnSuccessfully() {
        val telemetryClientUtil =
            TelemetryClientUtil(ApplicationProvider.getApplicationContext<Context>())
        val responseCode = StripeFireAndForgetRequestExecutor().execute(
            FingerprintRequest(telemetryClientUtil.createTelemetryMap(),
                UUID.randomUUID().toString()))
        assertEquals(200, responseCode)
    }
}
