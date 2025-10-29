package com.stripe.android.attestation.analytics

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
internal class DefaultAttestationAnalyticsEventsReporterTest {

    @Test
    fun testPrepare() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare")
    }

    @Test
    fun testPrepareFailed() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()
        ShadowSystemClock.advanceBy(10, TimeUnit.MILLISECONDS)

        eventsReporter.prepareFailed(error = RuntimeException("Test error"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.failed")
        assertThat(loggedParams["duration"]).isEqualTo(10)
        assertThat(loggedParams["error_message"]).isEqualTo("Test error")
    }

    @Test
    fun testPrepareSucceeded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()
        ShadowSystemClock.advanceBy(15, TimeUnit.MILLISECONDS)

        eventsReporter.prepareSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.succeeded")
        assertThat(loggedParams["duration"]).isEqualTo(15)
    }

    @Test
    fun testRequestToken() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token")
    }

    @Test
    fun testRequestTokenSucceeded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()
        ShadowSystemClock.advanceBy(8, TimeUnit.MILLISECONDS)

        eventsReporter.requestTokenSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"])
            .isEqualTo("elements.attestation.confirmation.request_token.succeeded")
        assertThat(loggedParams["duration"]).isEqualTo(8)
    }

    @Test
    fun testRequestTokenFailed() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()
        ShadowSystemClock.advanceBy(12, TimeUnit.MILLISECONDS)

        eventsReporter.requestTokenFailed(error = RuntimeException("Request failed"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token.failed")
        assertThat(loggedParams["duration"]).isEqualTo(12)
        assertThat(loggedParams["error_message"]).isEqualTo("Request failed")
    }

    private fun runScenario(
        durationProvider: DurationProvider = DefaultDurationProvider.instance,
        testBlock: (DefaultAttestationAnalyticsEventsReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val eventsReporter = DefaultAttestationAnalyticsEventsReporter(
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
