package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestExecutorTest {
    private val logger: Logger = mock()

    private val testDispatcher = TestCoroutineDispatcher()
    private val analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
        logger = logger,
        workContext = testDispatcher
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val analyticsRequest =
        AnalyticsRequestFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            .createPaymentMethodCreation(
                PaymentMethod.Type.Card,
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
