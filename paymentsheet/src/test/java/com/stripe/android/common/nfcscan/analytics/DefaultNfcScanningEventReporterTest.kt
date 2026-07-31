package com.stripe.android.common.nfcscan.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class DefaultNfcScanningEventReporterTest {
    @Test
    fun `onNfcScanStarted starts duration and fires event`() = runScenario {
        reporter.onNfcScanStarted()

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(DurationProvider.Key.NfcScan, reset = true)
            )
        ).isTrue()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_started")
    }

    @Test
    fun `onNfcScanSucceeded ends duration and fires event with duration and number of attempts`() = runScenario {
        durationProvider.start(DurationProvider.Key.NfcScan)

        reporter.onNfcScanSucceeded(numberOfAttempts = 2)

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.End(DurationProvider.Key.NfcScan)
            )
        ).isTrue()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_success")
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("number_of_attempts", 2)
    }

    @Test
    fun `onNfcScanAttemptStarted starts duration and fires event`() = runScenario {
        reporter.onNfcScanAttemptStarted()

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(DurationProvider.Key.NfcScanAttempt, reset = true)
            )
        ).isTrue()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_attempt_started")
    }

    @Test
    fun `onNfcScanAttemptSucceeded ends duration and fires event with duration`() = runScenario {
        durationProvider.start(DurationProvider.Key.NfcScanAttempt)

        reporter.onNfcScanAttemptSucceeded()

        assertThat(
            durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.NfcScanAttempt))
        ).isTrue()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_attempt_succeeded")
        assertThat(loggedParams).containsEntry("duration", 1.0f)
    }

    @Test
    fun `onNfcScanAttemptFailed ends duration and fires event with duration and error code`() = runScenario {
        durationProvider.start(DurationProvider.Key.NfcScanAttempt)

        reporter.onNfcScanAttemptFailed(errorCode = "expiredCard")

        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.NfcScanAttempt)))
            .isTrue()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_attempt_failed")
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("error_code", "expiredCard")
    }

    @Test
    fun `onNfcScanAttemptFailed includes parameters in analytics event`() = runScenario {
        durationProvider.start(DurationProvider.Key.NfcScanAttempt)

        reporter.onNfcScanAttemptFailed(
            errorCode = "nfcCardReadFailed",
            parameters = mapOf(
                "sw1" to "64",
                "sw2" to "00",
            ),
        )

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("error_code", "nfcCardReadFailed")
        assertThat(loggedParams).containsEntry("sw1", "64")
        assertThat(loggedParams).containsEntry("sw2", "00")
    }

    @Test
    fun `onNfcScanCancelled ends duration and fires event with cancellation reason and number of attempts`() =
        runScenario {
            reporter.onNfcScanCancelled(
                reason = NfcScanCancellationReason.Timeout,
                numberOfAttempts = 3,
            )

            assertThat(
                durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.NfcScan))
            ).isTrue()

            val loggedParams = executor.getExecutedRequests().single().params
            assertThat(loggedParams).containsEntry("event", "mc_nfc_scan_canceled")
            assertThat(loggedParams).containsEntry("duration", 1.0f)
            assertThat(loggedParams).containsEntry("cancellation_reason", "scanning_timeout")
            assertThat(loggedParams).containsEntry("number_of_attempts", 3)
        }

    private class Scenario(
        val reporter: NfcScanningEventReporter,
        val durationProvider: FakeDurationProvider,
        val executor: FakeAnalyticsRequestExecutor,
    )

    private fun runScenario(
        duration: kotlin.time.Duration = 1.seconds,
        block: Scenario.() -> Unit,
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider(duration = duration)
        val reporter = DefaultNfcScanningEventReporter(
            durationProvider = durationProvider,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null },
            ),
            eventPrefix = "mc_",
        )

        block(
            Scenario(
                reporter = reporter,
                durationProvider = durationProvider,
                executor = analyticsRequestExecutor,
            )
        )
    }
}
