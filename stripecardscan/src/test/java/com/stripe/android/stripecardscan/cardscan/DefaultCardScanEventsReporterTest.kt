package com.stripe.android.stripecardscan.cardscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
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
    fun testScanSucceeded() = runScenario(duration = 15.seconds) { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()

        defaultCardScanEventsReporter.scanSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_success")
        assertThat(loggedParams["duration"]).isEqualTo(15f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanFailed() = runScenario(duration = 11.seconds) { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()

        defaultCardScanEventsReporter.scanFailed(Throwable("oops"))

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_failed")
        assertThat(loggedParams["duration"]).isEqualTo(11f)
        assertThat(loggedParams["error_message"]).isEqualTo("unknown")
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanCancelledOnBackPressed() = runScenario(duration = 4.seconds) { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()

        defaultCardScanEventsReporter.scanCancelled(CancellationReason.Back)

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_cancel")
        assertThat(loggedParams["duration"]).isEqualTo(4f)
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
        duration: Duration? = null,
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
            durationProvider = FakeDurationProvider(duration),
            cardScanConfiguration = CardScanConfiguration(ELEMENTS_SESSION_ID)
        )

        testBlock(eventsReporter, analyticsRequestExecutor)
    }

    private class FakeDurationProvider(
        private val duration: Duration?
    ) : DurationProvider {
        override fun start(key: DurationProvider.Key, reset: Boolean) = Unit

        override fun end(key: DurationProvider.Key): Duration? = duration
    }

    companion object {
        private const val ELEMENTS_SESSION_ID = "elements_session_id"
    }
}
