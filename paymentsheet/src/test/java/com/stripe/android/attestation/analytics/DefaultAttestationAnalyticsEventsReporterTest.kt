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
import kotlin.time.Duration

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
        ShadowSystemClock.advanceBy(10, TimeUnit.SECONDS)

        eventsReporter.prepareFailed(error = RuntimeException("Test error"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.failed")
        assertThat(loggedParams["duration"]).isEqualTo(10000f)
        assertThat(loggedParams["error_message"]).isEqualTo("Test error")
    }

    @Test
    fun testPrepareFailedWithoutPrepare() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepareFailed(error = null)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.failed")
        assertThat(loggedParams).doesNotContainKey("duration")
        assertThat(loggedParams["error_message"]).isNull()
    }

    @Test
    fun testPrepareSucceeded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()
        ShadowSystemClock.advanceBy(15, TimeUnit.SECONDS)

        eventsReporter.prepareSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.succeeded")
        assertThat(loggedParams["duration"]).isEqualTo(15000f)
    }

    @Test
    fun testPrepareSucceededWithoutPrepare() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepareSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.prepare.succeeded")
        assertThat(loggedParams).doesNotContainKey("duration")
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
        ShadowSystemClock.advanceBy(8, TimeUnit.SECONDS)

        eventsReporter.requestTokenSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token.succeeded")
        assertThat(loggedParams["duration"]).isEqualTo(8000f)
    }

    @Test
    fun testRequestTokenSucceededWithoutRequestToken() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestTokenSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token.succeeded")
        assertThat(loggedParams).doesNotContainKey("duration")
    }

    @Test
    fun testRequestTokenFailed() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()
        ShadowSystemClock.advanceBy(12, TimeUnit.SECONDS)

        eventsReporter.requestTokenFailed(error = RuntimeException("Request failed"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token.failed")
        assertThat(loggedParams["duration"]).isEqualTo(12000f)
        assertThat(loggedParams["error_message"]).isEqualTo("Request failed")
    }

    @Test
    fun testRequestTokenFailedWithoutRequestToken() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestTokenFailed(error = null)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("elements.attestation.confirmation.request_token.failed")
        assertThat(loggedParams).doesNotContainKey("duration")
        assertThat(loggedParams["error_message"]).isNull()
    }

    @Test
    fun testPrepareStart() {
        var startCalled = false
        var keyUsed: DurationProvider.Key? = null
        val durationProvider = object : DurationProvider {
            override fun start(key: DurationProvider.Key, reset: Boolean) {
                startCalled = true
                keyUsed = key
            }

            override fun end(key: DurationProvider.Key): Duration? {
                return null
            }
        }
        runScenario(durationProvider) { eventsReporter, fakeAnalyticsRequestExecutor ->
            eventsReporter.prepare()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(startCalled).isTrue()
            assertThat(keyUsed).isEqualTo(DurationProvider.Key.PrepareAttestation)
        }
    }

    @Test
    fun testRequestTokenStart() {
        var startCalled = false
        var keyUsed: DurationProvider.Key? = null
        val durationProvider = object : DurationProvider {
            override fun start(key: DurationProvider.Key, reset: Boolean) {
                startCalled = true
                keyUsed = key
            }

            override fun end(key: DurationProvider.Key): Duration? {
                return null
            }
        }
        runScenario(durationProvider) { eventsReporter, fakeAnalyticsRequestExecutor ->
            eventsReporter.requestToken()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(startCalled).isTrue()
            assertThat(keyUsed).isEqualTo(DurationProvider.Key.Attest)
        }
    }

    private fun runScenario(
        durationProvider: DurationProvider = DefaultDurationProvider.instance,
        testBlock: (DefaultAttestationAnalyticsEventsReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) {
        // Clear any stale state from the singleton DurationProvider
        durationProvider.end(DurationProvider.Key.PrepareAttestation)
        durationProvider.end(DurationProvider.Key.Attest)

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
