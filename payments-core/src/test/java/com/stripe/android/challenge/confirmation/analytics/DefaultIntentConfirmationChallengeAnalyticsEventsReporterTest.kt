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
    fun testStart() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.start()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.start")
    }

    @Test
    fun testSuccess() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.start()
        ShadowSystemClock.advanceBy(10, TimeUnit.MILLISECONDS)

        eventsReporter.success()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.success")
        assertThat(loggedParams).containsEntry("duration", 10f)
    }

    @Test
    fun testError() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.start()
        ShadowSystemClock.advanceBy(15, TimeUnit.MILLISECONDS)

        eventsReporter.error(
            error = RuntimeException("Test error"),
            errorType = "runtime_error",
            errorCode = "test_code",
            fromBridge = true
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.error")
        assertThat(loggedParams).containsEntry("duration", 15f)
        assertThat(loggedParams).containsEntry("error_type", "runtime_error")
        assertThat(loggedParams).containsEntry("error_code", "test_code")
        assertThat(loggedParams).containsEntry("from_bridge", true)
    }

    @Test
    fun testErrorWithNullValues() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.start()
        ShadowSystemClock.advanceBy(20, TimeUnit.MILLISECONDS)

        eventsReporter.error(
            error = null,
            errorType = null,
            errorCode = null,
            fromBridge = false
        )

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.error")
        assertThat(loggedParams).containsEntry("duration", 20f)
        assertThat(loggedParams).containsEntry("error_type", null)
        assertThat(loggedParams).containsEntry("error_code", null)
        assertThat(loggedParams).containsEntry("from_bridge", false)
    }

    @Test
    fun testWebViewLoaded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.start()
        ShadowSystemClock.advanceBy(8, TimeUnit.MILLISECONDS)

        eventsReporter.webViewLoaded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams).containsEntry("event", "elements.intent_confirmation_challenge.web_view_loaded")
        assertThat(loggedParams).containsEntry("duration", 8f)
    }

    @Test
    fun testMultipleEvents() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        // Start the challenge
        eventsReporter.start()
        ShadowSystemClock.advanceBy(5, TimeUnit.MILLISECONDS)

        // WebView loads
        eventsReporter.webViewLoaded()
        ShadowSystemClock.advanceBy(5, TimeUnit.MILLISECONDS)

        // Challenge succeeds
        eventsReporter.success()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(3)

        // Verify start event
        assertThat(loggedRequests[0].params).containsEntry("event", "elements.intent_confirmation_challenge.start")

        // Verify webview loaded event with 5ms duration
        assertThat(loggedRequests[1].params)
            .containsEntry("event", "elements.intent_confirmation_challenge.web_view_loaded")
        assertThat(loggedRequests[1].params).containsEntry("duration", 5f)

        // Verify success event with 10ms total duration
        assertThat(loggedRequests[2].params).containsEntry("event", "elements.intent_confirmation_challenge.success")
        assertThat(loggedRequests[2].params).containsEntry("duration", 10f)
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
