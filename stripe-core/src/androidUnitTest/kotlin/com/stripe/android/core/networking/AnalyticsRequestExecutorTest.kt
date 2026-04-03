package com.stripe.android.core.networking

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor.Companion.FIELD_EVENT
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeNetworkClient: StripeNetworkClient = mock()

    private val analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
        stripeNetworkClient = stripeNetworkClient,
        logger = logger,
        workContext = testDispatcher
    )

    private val analyticsRequest = AnalyticsRequest(mapOf(FIELD_EVENT to TEST_EVENT), emptyMap())

    @Test
    fun `executeAsync should log and delegate to stripeNetworkClient`() =
        runTest {
            analyticsRequestExecutor.executeAsync(analyticsRequest)

            verify(logger).info(
                "Event: $TEST_EVENT"
            )
            verify(stripeNetworkClient).executeRequest(same(analyticsRequest))
        }

    @Test
    fun `executeAsync should log error when stripeNetworkClient fails`() =
        runTest {
            val expectedException = APIConnectionException("something went wrong")
            whenever(stripeNetworkClient.executeRequest(any())).thenThrow(expectedException)
            analyticsRequestExecutor.executeAsync(analyticsRequest)

            verify(logger).info(
                "Event: $TEST_EVENT"
            )
            verify(stripeNetworkClient).executeRequest(same(analyticsRequest))
            verify(logger).error(
                eq("Exception while making analytics request"),
                same(expectedException)
            )
        }

    private companion object {
        const val TEST_EVENT = "TEST_EVENT"
    }
}
