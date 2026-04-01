package com.stripe.android.challenge.confirmation.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class DefaultIntentConfirmationChallengeAnalyticsEventsReporterTest {

    @Test
    fun testOnStart() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.start")
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    @Test
    fun testOnSuccess() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")
        ShadowSystemClock.advanceBy(10, TimeUnit.MILLISECONDS)

        eventsReporter.onSuccess(captchaVendorName = "hcaptcha")

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.success")
        assertThat(loggedParams).containsEntry("duration", 10f)
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    @Test
    fun testOnError() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")
        ShadowSystemClock.advanceBy(15, TimeUnit.MILLISECONDS)

        eventsReporter.onError(
            errorType = "runtime_error",
            errorCode = "test_code",
            fromBridge = true,
            captchaVendorName = "hcaptcha"
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.error")
        assertThat(loggedParams).containsEntry("duration", 15f)
        assertThat(loggedParams).containsEntry("error_type", "runtime_error")
        assertThat(loggedParams).containsEntry("error_code", "test_code")
        assertThat(loggedParams).containsEntry("from_bridge", true)
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    @Test
    fun testOnErrorWithNullValues() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")
        ShadowSystemClock.advanceBy(20, TimeUnit.MILLISECONDS)

        eventsReporter.onError(
            errorType = null,
            errorCode = null,
            fromBridge = false,
            captchaVendorName = "hcaptcha"
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.error")
        assertThat(loggedParams).containsEntry("duration", 20f)
        assertThat(loggedParams).containsEntry("error_type", null)
        assertThat(loggedParams).containsEntry("error_code", null)
        assertThat(loggedParams).containsEntry("from_bridge", false)
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    @Test
    fun testOnCancel() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")
        ShadowSystemClock.advanceBy(12, TimeUnit.MILLISECONDS)

        eventsReporter.onCancel(captchaVendorName = "hcaptcha")

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.cancel")
        assertThat(loggedParams).containsEntry("duration", 12f)
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    @Test
    fun testOnWebViewLoaded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.onStart(captchaVendorName = "hcaptcha")
        ShadowSystemClock.advanceBy(8, TimeUnit.MILLISECONDS)

        eventsReporter.onWebViewLoaded(captchaVendorName = "hcaptcha")

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.web_view_loaded")
        assertThat(loggedParams).containsEntry("duration", 8f)
        assertThat(loggedParams).containsEntry("captcha_vendor_name", "hcaptcha")
    }

    private fun runScenario(
        durationProvider: DurationProvider = DefaultDurationProvider.instance,
        testBlock: (DefaultIntentConfirmationChallengeAnalyticsEventReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val eventsReporter = DefaultIntentConfirmationChallengeAnalyticsEventReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null }
            ),
            durationProvider = durationProvider
        )

        testBlock(eventsReporter, analyticsRequestExecutor)
    }
}
