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
import kotlinx.coroutines.test.runTest
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

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.signup.failure")
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

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.signup.failure")
            assertThat(loggedParams.get("error_message")).isEqualTo(expectedMessage)
            assertThat(loggedParams.get("analytics_value")).isEqualTo("apiError")
        }

    @Test
    fun onAccountLookupComplete_withCheckoutSession_includesCheckoutSessionId() =
        runScenario(
            checkoutSessionId = CHECKOUT_SESSION_ID,
        ) { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.onAccountLookupComplete()

            testScheduler.advanceUntilIdle()

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.account_lookup.complete")
            assertThat(loggedParams["checkout_session_id"]).isEqualTo(CHECKOUT_SESSION_ID)
        }

    @Test
    fun onAccountLookupComplete_withoutCheckoutSession_omitsCheckoutSessionId() =
        runScenario(checkoutSessionId = null) { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.onAccountLookupComplete()

            testScheduler.advanceUntilIdle()

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.account_lookup.complete")
            assertThat(loggedParams).doesNotContainKey("checkout_session_id")
        }

    @Test
    fun onPopupShow_withCheckoutSession_includesCheckoutSessionId() =
        runScenario(checkoutSessionId = null) { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.onPopupShow(checkoutSessionId = CHECKOUT_SESSION_ID)

            testScheduler.advanceUntilIdle()

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.popup.show")
            assertThat(loggedParams["checkout_session_id"]).isEqualTo(CHECKOUT_SESSION_ID)
        }

    @Test
    fun on2FAResendCode_withoutCheckoutSessionId_fallsBackToReporterCheckoutSessionId() =
        runScenario(
            checkoutSessionId = CHECKOUT_SESSION_ID,
        ) { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.on2FAResendCode(verificationType = "EMAIL", checkoutSessionId = null)

            testScheduler.advanceUntilIdle()

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.2fa.resend_code")
            assertThat(loggedParams["checkout_session_id"]).isEqualTo(CHECKOUT_SESSION_ID)
        }

    @Test
    fun onInlineSignupCheckboxChecked_withCheckoutSession_includesCheckoutSessionId() =
        runScenario(checkoutSessionId = null) { linkEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            linkEventsReporter.onInlineSignupCheckboxChecked(checkoutSessionId = CHECKOUT_SESSION_ID)

            testScheduler.advanceUntilIdle()

            val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("link.signup.checkbox_checked")
            assertThat(loggedParams["checkout_session_id"]).isEqualTo(CHECKOUT_SESSION_ID)
        }

    private fun runScenario(
        checkoutSessionId: String? = null,
        testBlock: suspend (LinkEventsReporter, FakeAnalyticsRequestExecutor, TestCoroutineScheduler) -> Unit
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
            durationProvider = FakeDurationProvider(),
            checkoutSessionId = checkoutSessionId,
        )

        testScope.runTest {
            testBlock(linkEventsReporter, analyticsRequestExecutor, testScope.testScheduler)
        }

        analyticsRequestExecutor.validate()
    }

    private companion object {
        const val CHECKOUT_SESSION_ID = "cs_test_123"
    }

    class FakeDurationProvider : DurationProvider {
        override fun start(key: DurationProvider.Key, reset: Boolean) {
            // Do nothing.
        }

        override fun elapsed(key: DurationProvider.Key): Duration? {
            return Duration.ZERO
        }

        override fun end(key: DurationProvider.Key): Duration? {
            return Duration.ZERO
        }

        override fun completedDuration(key: DurationProvider.Key): Duration? {
            throw NotImplementedError("completedDuration is not implemented in FakeDurationProvider")
        }

        override suspend fun <T> measureDuration(
            key: DurationProvider.Key,
            block: suspend () -> T,
        ): T {
            start(key)
            return try {
                block()
            } finally {
                end(key)
            }
        }
    }
}
