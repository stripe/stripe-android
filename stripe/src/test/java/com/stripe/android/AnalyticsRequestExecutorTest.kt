package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestExecutorTest {

    private val analyticsRequestExecutor = AnalyticsRequestExecutor.Default()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Test to verify fingerprint endpoint's success
    @Test
    fun execute_withFingerprintRequest_shouldReturnSuccessfully() {
        val responseCode = analyticsRequestExecutor.execute(
            AnalyticsRequest.Factory().create(
                AnalyticsDataFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                    .createPaymentMethodCreationParams(
                        "pm_12345",
                        PaymentMethod.Type.Card,
                        emptySet()
                    ),
                ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            )
        )
        assertThat(responseCode)
            .isEqualTo(200)
    }
}
