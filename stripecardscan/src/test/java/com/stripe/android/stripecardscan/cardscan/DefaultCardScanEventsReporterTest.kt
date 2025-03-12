package com.stripe.android.stripecardscan.cardscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import kotlin.time.Duration

internal class DefaultCardScanEventsReporterTest {

    @Test
    fun testScanStarted() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_scan_started")
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanSucceeded() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_success")
        assertThat(loggedParams["duration"]).isEqualTo(0f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanFailed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanFailed(Throwable("oops"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_failed")
        assertThat(loggedParams["duration"]).isEqualTo(0f)
        assertThat(loggedParams["error_message"]).isEqualTo("unknown")
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanCancelledOnBackPressed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanCancelled(CancellationReason.Back)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_cancel")
        assertThat(loggedParams["duration"]).isEqualTo(0f)
        assertThat(loggedParams["cancellation_reason"]).isEqualTo("back")
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanCancelledOnCameraPermissionDenied() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.CameraPermissionDenied)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(loggedRequests.first().params["cancellation_reason"])
                .isEqualTo("camera_permission_denied")
        }

    @Test
    fun testScanCancelledOnClosed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanCancelled(CancellationReason.Closed)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        assertThat(loggedRequests.first().params["cancellation_reason"])
            .isEqualTo("closed")
    }

    @Test
    fun testScanCancelledOnUserCannotScan() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.UserCannotScan)

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(loggedRequests.first().params["cancellation_reason"])
                .isEqualTo("user_cannot_scan")
        }

    private fun runScenario(
        testBlock: (DefaultCardScanEventsReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val eventsReporter = DefaultCardScanEventsReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null }
            ),
            durationProvider = FakeDurationProvider(),
            cardScanConfiguration = CardScanConfiguration(ELEMENTS_SESSION_ID)
        )

        testBlock(eventsReporter, analyticsRequestExecutor)
    }

    companion object {
        private const val ELEMENTS_SESSION_ID = "elements_session_id"
    }

    private class FakeDurationProvider : DurationProvider {
        override fun start(key: DurationProvider.Key, reset: Boolean) = Unit

        override fun end(key: DurationProvider.Key): Duration {
            return Duration.ZERO
        }
    }
}
