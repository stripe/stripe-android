package com.stripe.android.link.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.IllegalStateException
import kotlin.test.Test
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
class DefaultLinkEventsReporterTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun onLinkSignupFailure_sendsCorrectAnalytics() =
        runScenario { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.onSignupFailure(isInline = false, error = IllegalStateException("An error occurred"))

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams.get("event")).isEqualTo("link.signup.failure")
            assertThat(loggedParams.get("error_message")).isEqualTo("unknown")
            assertThat(loggedParams.get("analytics_value")).isEqualTo("java.lang.IllegalStateException")
        }

    @Test
    fun onLinkSignupFailure_withApiException_sendsCorrectAnalytics() =
        runScenario { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            val expectedMessage = "ApiException error message"
            val apiException = APIException(
                stripeError = StripeError(message = expectedMessage)
            )
            linkEventsReporter.onSignupFailure(isInline = false, error = apiException)

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams.get("event")).isEqualTo("link.signup.failure")
            assertThat(loggedParams.get("error_message")).isEqualTo(expectedMessage)
            assertThat(loggedParams.get("analytics_value")).isEqualTo("apiError")
        }

    private fun runScenario(
        testBlock: (LinkEventsReporter, FakeAnalyticsRequestExecutor, TestCoroutineScheduler) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val testScope = TestScope()
        val linkEventsReporter = DefaultLinkEventsReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = application,
                publishableKey = "pk_1234"
            ),
            errorReporter = FakeErrorReporter(),
            workContext = testScope.coroutineContext,
            logger = Logger.noop(),
            durationProvider = FakeDurationProvider()
        )

        testBlock(linkEventsReporter, analyticsRequestExecutor, testScope.testScheduler)
    }

    class FakeDurationProvider : DurationProvider {
        override fun start(key: DurationProvider.Key, reset: Boolean) {
            // Do nothing.
        }

        override fun end(key: DurationProvider.Key): Duration? {
            return Duration.ZERO
        }
    }
}
