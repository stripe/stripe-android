package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestExecutorTest {
    private val logger: Logger = mock()

    private val testDispatcher = TestCoroutineDispatcher()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor.Default(
        logger = logger,
        workContext = testDispatcher
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val analyticsRequest =
        AnalyticsRequestFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            .createPaymentMethodCreation(
                PaymentMethodCreateParams.Type.Card,
                emptySet()
            )

    @Test
    fun execute_shouldReturnSuccessfully() {
        val responseCode = analyticsRequestExecutor.execute(
            analyticsRequest
        )
        assertThat(responseCode)
            .isEqualTo(200)
    }

    @Test
    fun execute_shouldReturnSuccessfull2y() {
        analyticsRequestExecutor.executeAsync(analyticsRequest)
        verify(logger).info(
            "Event: stripe_android.payment_method_creation"
        )
    }
}
