package com.stripe.android.hcaptcha.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
internal class DefaultCaptchaEventsReporterTest {

    @Test
    fun testInit() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.init(SITE_KEY)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.init")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
    }

    @Test
    fun testExecute() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.execute(SITE_KEY)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.execute")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
    }

    @Test
    fun testSuccess() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.init(SITE_KEY)
        ShadowSystemClock.advanceBy(15, TimeUnit.SECONDS)

        defaultChallengeEventsReporter.success(
            SITE_KEY,
            resultImmediatelyAvailable = false,
            duration = Duration.parse("15s")
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.success")
        assertThat(loggedParams["duration"]).isEqualTo(15000f)
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)
    }

    @Test
    fun testSuccessWithoutInit() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.success(
            SITE_KEY,
            resultImmediatelyAvailable = false,
            duration = null
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.success")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)
        assertThat(loggedParams).doesNotContainKey("duration")
    }

    @Test
    fun testError() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
        defaultChallengeEventsReporter.init(SITE_KEY)
        ShadowSystemClock.advanceBy(11, TimeUnit.SECONDS)

        defaultChallengeEventsReporter.error(
            Throwable("test error"),
            SITE_KEY,
            resultImmediatelyAvailable = false,
            duration = Duration.parse("11s")
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
        assertThat(loggedParams["duration"]).isEqualTo(11000f)
        assertThat(loggedParams["error_message"]).isEqualTo("test error")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)

        // Verify unexpected error was reported
        assertThat(fakeErrorReporter.getLoggedErrors())
            .contains(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE.eventName)
    }

    @Test
    fun testErrorWithNullThrowable() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.init(SITE_KEY)
            ShadowSystemClock.advanceBy(8, TimeUnit.SECONDS)

            defaultChallengeEventsReporter.error(
                null,
                SITE_KEY,
                resultImmediatelyAvailable = false,
                duration = Duration.parse("8s")
            )

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(2)
            val loggedParams = loggedRequests.last().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["duration"]).isEqualTo(8000f)
            assertThat(loggedParams["error_message"]).isNull()
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
            assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)

            // Verify unexpected error was reported for null error
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE.eventName)
        }

    @Test
    fun testErrorWithoutInit() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.error(
                Throwable("test error"),
                SITE_KEY,
                resultImmediatelyAvailable = false,
                duration = Duration.ZERO
            )

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["error_message"]).isEqualTo("test error")
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
            assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)
            assertThat(loggedParams["duration"]).isEqualTo(0f)

            // Verify unexpected error was reported
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE.eventName)
        }

    @Test
    fun testErrorWithHCaptchaException() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.init(SITE_KEY)
            ShadowSystemClock.advanceBy(12, TimeUnit.SECONDS)

            val hCaptchaException = HCaptchaException(HCaptchaError.NETWORK_ERROR, "Network error")
            defaultChallengeEventsReporter.error(
                hCaptchaException,
                SITE_KEY,
                resultImmediatelyAvailable = false,
                duration = Duration.parse("12s")
            )

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(2)
            val loggedParams = loggedRequests.last().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["duration"]).isEqualTo(12000f)
            assertThat(loggedParams["error_message"]).isEqualTo("Network error")
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
            assertThat(loggedParams["result_immediately_available"]).isEqualTo(false)

            // Verify expected error was reported for HCaptchaException
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.ExpectedErrorEvent.HCAPTCHA_FAILURE.eventName)
        }

    private fun runScenario(
        testBlock: (DefaultCaptchaEventsReporter, FakeAnalyticsRequestExecutor, FakeErrorReporter) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val fakeErrorReporter = FakeErrorReporter()
        val eventsReporter = DefaultCaptchaEventsReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null }
            ),
            errorReporter = fakeErrorReporter
        )

        testBlock(eventsReporter, analyticsRequestExecutor, fakeErrorReporter)
    }

    companion object {
        private const val SITE_KEY = "test_site_key"
    }
}
