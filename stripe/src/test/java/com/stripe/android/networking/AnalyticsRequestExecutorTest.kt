package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestExecutorTest {

    private val analyticsRequestExecutor = AnalyticsRequestExecutor.Default()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun execute_shouldReturnSuccessfully() {
        val responseCode = analyticsRequestExecutor.execute(
            AnalyticsRequest.Factory().create(
                AnalyticsDataFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                    .createPaymentMethodCreationParams(
                        PaymentMethodCreateParams.Type.Card,
                        emptySet(),
                        RequestId("req_123")
                    )
            )
        )
        assertThat(responseCode)
            .isEqualTo(200)
    }
}
