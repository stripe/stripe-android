package com.stripe.android.stripecardscan.cardscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import kotlin.time.Duration

internal class DefaultCardScanEventsReporterTest {

    @Test
    fun testScanStarted() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
        defaultCardScanEventsReporter.scanStarted()

        testScheduler.advanceUntilIdle()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("card_scan.scan_started")
    }

    @Test
    fun testScanSucceeded() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            defaultCardScanEventsReporter.scanSucceeded()

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams["event"]).isEqualTo("card_scan.scan_succeeded")
            assertThat(loggedParams["duration"]).isEqualTo(0f)
        }

    @Test
    fun testScanFailed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
        defaultCardScanEventsReporter.scanFailed(Throwable("oops"))

        testScheduler.advanceUntilIdle()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(1)
        val loggedParams = loggedRequests.first().params
        assertThat(loggedParams["event"]).isEqualTo("card_scan.scan_failed")
        assertThat(loggedParams["duration"]).isEqualTo(0f)
        assertThat(loggedParams["error_message"]).isEqualTo("unknown")
    }

    @Test
    fun testScanCancelledOnBackPressed() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.Back)

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            val loggedParams = loggedRequests.first().params
            assertThat(loggedParams["event"]).isEqualTo("card_scan.scan_cancelled")
            assertThat(loggedParams["duration"]).isEqualTo(0f)
            assertThat(loggedParams["cancellation_reason"]).isEqualTo("back")
        }

    @Test
    fun testScanCancelledOnCameraPermissionDenied() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.CameraPermissionDenied)

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(loggedRequests.first().params["cancellation_reason"])
                .isEqualTo("camera_permission_denied")
        }

    @Test
    fun testScanCancelledOnClosed() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.Closed)

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(loggedRequests.first().params["cancellation_reason"])
                .isEqualTo("closed")
        }

    @Test
    fun testScanCancelledOnUserCannotScan() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor, testScheduler ->
            defaultCardScanEventsReporter.scanCancelled(CancellationReason.UserCannotScan)

            testScheduler.advanceUntilIdle()

            val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

            assertThat(loggedRequests).hasSize(1)
            assertThat(loggedRequests.first().params["cancellation_reason"])
                .isEqualTo("user_cannot_scan")
        }

    private fun runScenario(
        testBlock: (DefaultCardScanEventsReporter, FakeAnalyticsRequestExecutor, TestCoroutineScheduler) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val testScope = TestScope()
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
            workContext = testScope.coroutineContext,
            durationProvider = FakeDurationProvider()
        )

        testBlock(eventsReporter, analyticsRequestExecutor, testScope.testScheduler)
    }

    private class FakeDurationProvider : DurationProvider {
        override fun start(key: DurationProvider.Key, reset: Boolean) = Unit

        override fun end(key: DurationProvider.Key): Duration {
            return Duration.ZERO
        }
    }
}
