package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestExecutorTest {
    private val logger: Logger = mock()
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripeNetworkClient: StripeNetworkClient = mock()

    private val analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
        stripeNetworkClient = stripeNetworkClient,
        logger = logger,
        workContext = testDispatcher
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val analyticsRequest =
        PaymentAnalyticsRequestFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            .createPaymentMethodCreation(
                PaymentMethod.Type.Card,
                emptySet()
            )

    @Test
    fun `executeAsync should log and delegate to stripeNetworkClient`() =
        testDispatcher.runBlockingTest {
            analyticsRequestExecutor.executeAsync(analyticsRequest)

            verify(logger).info(
                "Event: stripe_android.payment_method_creation"
            )
            verify(stripeNetworkClient).executeRequest(same(analyticsRequest))
        }

    @Test
    fun `executeAsync should log error when stripeNetworkClient fails`() =
        testDispatcher.runBlockingTest {
            val expectedException = APIConnectionException("something went wrong")
            whenever(stripeNetworkClient.executeRequest(any())).thenThrow(expectedException)
            analyticsRequestExecutor.executeAsync(analyticsRequest)

            verify(logger).info(
                "Event: stripe_android.payment_method_creation"
            )
            verify(stripeNetworkClient).executeRequest(same(analyticsRequest))
            verify(logger).error(
                eq("Exception while making analytics request"),
                same(expectedException)
            )
        }
}
