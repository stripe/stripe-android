package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeFireAndForgetRequestExecutorTest {

    // Test to verify fingerprint endpoint's success
    @Test
    fun execute_withFingerprintRequest_shouldReturnSuccessfully() {
        val fingerprintRequestParamsFactory =
            FingerprintRequestParamsFactory(ApplicationProvider.getApplicationContext())
        val responseCode = StripeFireAndForgetRequestExecutor().execute(
            FingerprintRequest(
                params = fingerprintRequestParamsFactory.createParams(),
                guid = UUID.randomUUID().toString()
            )
        )
        assertEquals(200, responseCode)
    }
}
