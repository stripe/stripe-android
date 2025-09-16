package com.stripe.android.hcaptcha.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
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

        defaultChallengeEventsReporter.success(SITE_KEY)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.success")
        assertThat(loggedParams["duration"]).isEqualTo(15000f)
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
    }

    @Test
    fun testSuccessWithoutInit() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.success(SITE_KEY)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.success")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams).doesNotContainKey("duration")
    }

    @Test
    fun testError() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.init(SITE_KEY)
            ShadowSystemClock.advanceBy(11, TimeUnit.SECONDS)

            defaultChallengeEventsReporter.error(Throwable("test error"), SITE_KEY)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(2)
            val loggedParams = loggedRequests.last().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["duration"]).isEqualTo(11000f)
            assertThat(loggedParams["error_message"]).isEqualTo("test error")
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)

            // Verify unexpected error was reported
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE.eventName)
        }

    @Test
    fun testErrorWithNullThrowable() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.init(SITE_KEY)
            ShadowSystemClock.advanceBy(8, TimeUnit.SECONDS)

            defaultChallengeEventsReporter.error(null, SITE_KEY)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(2)
            val loggedParams = loggedRequests.last().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["duration"]).isEqualTo(8000f)
            assertThat(loggedParams["error_message"]).isNull()
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)

            // Verify unexpected error was reported for null error
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE.eventName)
        }

    @Test
    fun testErrorWithoutInit() =
        runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, fakeErrorReporter ->
            defaultChallengeEventsReporter.error(Throwable("test error"), SITE_KEY)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["error_message"]).isEqualTo("test error")
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
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
            defaultChallengeEventsReporter.error(hCaptchaException, SITE_KEY)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(2)
            val loggedParams = loggedRequests.last().params
            assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.error")
            assertThat(loggedParams["duration"]).isEqualTo(12000f)
            assertThat(loggedParams["error_message"]).isEqualTo("Network error")
            assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)

            // Verify expected error was reported for HCaptchaException
            assertThat(fakeErrorReporter.getLoggedErrors())
                .contains(ErrorReporter.ExpectedErrorEvent.HCAPTCHA_FAILURE.eventName)
        }

    @Test
    fun testAttachStart() {
        var startCalled = false
        val durationProvider = object : DurationProvider {
            override fun start(key: DurationProvider.Key, reset: Boolean) {
                startCalled = true
            }

            override fun end(key: DurationProvider.Key): Duration? {
                throw NotImplementedError("this function should not be called")
            }
        }
        runScenario(durationProvider) { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
            defaultChallengeEventsReporter.attachStart()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).isEmpty()
            assertThat(startCalled).isTrue()
        }
    }

    @Test
    fun testAttachEnd() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.attachStart()
        ShadowSystemClock.advanceBy(5, TimeUnit.MILLISECONDS)
        defaultChallengeEventsReporter.attachEnd(SITE_KEY, isReady = true)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.attach")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams["is_ready"]).isEqualTo(true)
        assertThat(loggedParams["duration"]).isEqualTo(5)
    }

    @Test
    fun testAttachEndWithFalseReady() = runScenario { defaultChallengeEventsReporter, fakeAnalyticsRequestExecutor, _ ->
        defaultChallengeEventsReporter.attachStart()
        ShadowSystemClock.advanceBy(5, TimeUnit.MILLISECONDS)
        defaultChallengeEventsReporter.attachEnd(SITE_KEY, isReady = false)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.captcha.passive.attach")
        assertThat(loggedParams["site_key"]).isEqualTo(SITE_KEY)
        assertThat(loggedParams["is_ready"]).isEqualTo(false)
        assertThat(loggedParams["duration"]).isEqualTo(5)
    }

    private fun runScenario(
        durationProvider: DurationProvider = DefaultDurationProvider.instance,
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
            durationProvider = durationProvider,
            errorReporter = fakeErrorReporter
        )

        testBlock(eventsReporter, analyticsRequestExecutor, fakeErrorReporter)
    }

    companion object {
        private const val SITE_KEY = "test_site_key"
    }
}
