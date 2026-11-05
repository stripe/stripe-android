package com.stripe.android.attestation.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.attestation.AttestationError
import kotlinx.coroutines.test.runTest
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

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare")
    }

    @Test
    fun testPrepareFailed() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()
        ShadowSystemClock.advanceBy(10, TimeUnit.MILLISECONDS)

        eventsReporter.prepareFailed(error = RuntimeException("Test error"))

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare.failed")
        assertThat(loggedParams).containsEntry("duration", 10f)
        assertThat(loggedParams).containsEntry("error_message", "Test error")
        assertThat(loggedParams).doesNotContainKey("android_attestation_error_type")
        assertThat(loggedParams).doesNotContainKey("android_attestation_error_retriable")
    }

    @Test
    fun testPrepareFailedWithAttestationError() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()

        eventsReporter.prepareFailed(
            error = AttestationError(
                errorType = AttestationError.ErrorType.API_NOT_AVAILABLE,
                message = "Attestation API is unavailable"
            )
        )

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare.failed")
        assertThat(loggedParams).containsEntry("android_attestation_error_type", "API_NOT_AVAILABLE")
        assertThat(loggedParams).containsEntry("android_attestation_error_retriable", false)
    }

    @Test
    fun testPrepareSucceeded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.prepare()
        ShadowSystemClock.advanceBy(15, TimeUnit.MILLISECONDS)

        eventsReporter.prepareSucceeded()

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.prepare")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent(
            "elements.attestation.confirmation.prepare.succeeded"
        )
        assertThat(loggedParams).containsEntry("duration", 15f)
    }

    @Test
    fun testRequestToken() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.request_token")
    }

    @Test
    fun testRequestTokenSucceeded() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()
        ShadowSystemClock.advanceBy(8, TimeUnit.MILLISECONDS)

        eventsReporter.requestTokenSucceeded()

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.request_token")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent(
            "elements.attestation.confirmation.request_token.succeeded"
        )
        assertThat(loggedParams).containsEntry("duration", 8f)
    }

    @Test
    fun testRequestTokenFailed() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()
        ShadowSystemClock.advanceBy(12, TimeUnit.MILLISECONDS)

        eventsReporter.requestTokenFailed(error = RuntimeException("Request failed"))

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.request_token")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent(
            "elements.attestation.confirmation.request_token.failed"
        )
        assertThat(loggedParams).containsEntry("duration", 12f)
        assertThat(loggedParams).containsEntry("error_message", "Request failed")
        assertThat(loggedParams).doesNotContainKey("android_attestation_error_type")
        assertThat(loggedParams).doesNotContainKey("android_attestation_error_retriable")
    }

    @Test
    fun testRequestTokenFailedWithAttestationError() = runScenario { eventsReporter, fakeAnalyticsRequestExecutor ->
        eventsReporter.requestToken()

        eventsReporter.requestTokenFailed(
            error = AttestationError(
                errorType = AttestationError.ErrorType.NETWORK_ERROR,
                message = "Network error"
            )
        )

        fakeAnalyticsRequestExecutor.awaitEvent("elements.attestation.confirmation.request_token")
        val loggedParams = fakeAnalyticsRequestExecutor.awaitEvent(
            "elements.attestation.confirmation.request_token.failed"
        )
        assertThat(loggedParams).containsEntry("android_attestation_error_type", "NETWORK_ERROR")
        assertThat(loggedParams).containsEntry("android_attestation_error_retriable", true)
    }

    private fun runScenario(
        durationProvider: DurationProvider = DefaultDurationProvider.instance,
        testBlock: suspend (DefaultAttestationAnalyticsEventsReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) = runTest {
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

        analyticsRequestExecutor.validate()
    }
}
