package com.stripe.android.stripecardscan.cardscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

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
    fun testScanSucceeded() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(15, TimeUnit.SECONDS)

        defaultCardScanEventsReporter.scanSucceeded()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_success")
        assertThat(loggedParams["duration"]).isEqualTo(15f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testScanFailed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(11, TimeUnit.SECONDS)

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
    fun testScanCancelledOnBackPressed() = runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(4, TimeUnit.SECONDS)

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

    @Test
    fun testMlKitFoundPanSentOncePerSession() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(2, TimeUnit.SECONDS)

        defaultCardScanEventsReporter.scanMlKitFoundPan()
        defaultCardScanEventsReporter.scanMlKitFoundPan()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_mlkit_found_pan")
        assertThat(loggedParams["duration"]).isEqualTo(2f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testMlKitFoundExpSentOncePerSession() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(3, TimeUnit.SECONDS)

        defaultCardScanEventsReporter.scanMlKitFoundExp()
        defaultCardScanEventsReporter.scanMlKitFoundExp()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_mlkit_found_exp")
        assertThat(loggedParams["duration"]).isEqualTo(3f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testDarkniteFoundPanSentOncePerSession() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(4, TimeUnit.SECONDS)

        defaultCardScanEventsReporter.scanDarkniteFoundPan()
        defaultCardScanEventsReporter.scanDarkniteFoundPan()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_darknite_found_pan")
        assertThat(loggedParams["duration"]).isEqualTo(4f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testModelsDisagreeSentOncePerSession() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanStarted()
        ShadowSystemClock.advanceBy(5, TimeUnit.SECONDS)

        defaultCardScanEventsReporter.scanModelsDisagree()
        defaultCardScanEventsReporter.scanModelsDisagree()

        val loggedRequests = fakeAnalyticsRequestExecutor.getExecutedRequests()

        assertThat(loggedRequests).hasSize(2)
        val loggedParams = loggedRequests.last().params
        assertThat(loggedParams["event"]).isEqualTo("cardscan_models_disagree")
        assertThat(loggedParams["duration"]).isEqualTo(5f)
        assertThat(loggedParams["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID)
    }

    @Test
    fun testMilestonesDoNotEmitWithoutActiveSession() =
        runScenario { defaultCardScanEventsReporter, fakeAnalyticsRequestExecutor ->
        defaultCardScanEventsReporter.scanMlKitFoundPan()
        defaultCardScanEventsReporter.scanMlKitFoundExp()
        defaultCardScanEventsReporter.scanDarkniteFoundPan()
        defaultCardScanEventsReporter.scanModelsDisagree()

        assertThat(fakeAnalyticsRequestExecutor.getExecutedRequests()).isEmpty()
    }

    @Test
    fun `original scan events share attempt and model attribution`() = runScenario { reporter, executor ->
        reporter.scanStarted()
        reporter.scanDarkniteFoundPan()
        reporter.scanSucceeded()

        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events).hasSize(3)
        val attempt = events.first()["scan_session_id"]
        assertThat(attempt).isNotNull()
        events.forEach {
            assertThat(it["scan_session_id"]).isEqualTo(attempt)
            assertThat(it["ssd_model"]).isEqualTo("original")
            assertThat(it["ssd_model_version"]).isEqualTo("1.1.1.16")
            assertThat(it["ssd_ocr_enabled"]).isEqualTo(true)
            assertThat(it["mlkit_enabled"]).isEqualTo(false)
        }
    }

    @Test
    fun `compact failure retains assignment and ML Kit setting`() = runScenario(
        configuration = CardScanConfiguration(
            elementsSessionId = ELEMENTS_SESSION_ID,
            useCompactSsdOcr = true,
            enableMlKitTextRecognition = true,
        ),
    ) { reporter, executor ->
        reporter.scanStarted()
        reporter.scanFailed(null)
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events).hasSize(2)
        events.forEach {
            assertThat(it["ssd_model"]).isEqualTo("compact")
            assertThat(it["ssd_model_version"]).isEqualTo("embossed-fine-w8-2026-11-02")
            assertThat(it["ssd_ocr_enabled"]).isEqualTo(true)
            assertThat(it["mlkit_enabled"]).isEqualTo(true)
        }
        assertThat(events.first()["scan_session_id"]).isEqualTo(events.last()["scan_session_id"])
    }

    @Test
    fun `permission cancellation records assignment but SSD disabled`() = runScenario(
        configuration = CardScanConfiguration(
            elementsSessionId = ELEMENTS_SESSION_ID,
            useCompactSsdOcr = true,
            disableSsdOcr = true,
        ),
    ) { reporter, executor ->
        reporter.scanCancelled(CancellationReason.CameraPermissionDenied)
        val event = executor.getExecutedRequests().single().params
        assertThat(event["ssd_model"]).isEqualTo("compact")
        assertThat(event["ssd_ocr_enabled"]).isEqualTo(false)
        assertThat(event["scan_session_id"]).isNotNull()
    }

    @Test
    fun `repeated scans in one Elements Session receive distinct attempt IDs`() = runScenario { reporter, executor ->
        reporter.scanStarted()
        reporter.scanCancelled(CancellationReason.Back)
        reporter.scanPresented()
        reporter.scanStarted()
        reporter.scanSucceeded()
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events).hasSize(5)
        assertThat(events[0]["scan_session_id"]).isEqualTo(events[1]["scan_session_id"])
        assertThat(events[2]["scan_session_id"]).isEqualTo(events[4]["scan_session_id"])
        assertThat(events[0]["scan_session_id"]).isNotEqualTo(events[2]["scan_session_id"])
        events.forEach { assertThat(it["elements_session_id"]).isEqualTo(ELEMENTS_SESSION_ID) }
    }

    @Test
    fun `camera stream restart preserves attempt and elapsed duration`() = runScenario { reporter, executor ->
        reporter.scanStarted()
        ShadowSystemClock.advanceBy(3, TimeUnit.SECONDS)
        reporter.scanStarted()
        ShadowSystemClock.advanceBy(2, TimeUnit.SECONDS)
        reporter.scanSucceeded()

        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events).hasSize(2)
        assertThat(events.first()["scan_session_id"]).isEqualTo(events.last()["scan_session_id"])
        assertThat(events.last()["duration"]).isEqualTo(5f)
    }

    @Test
    fun `presentation includes attempts canceled before camera starts`() = runScenario { reporter, executor ->
        reporter.scanPresented()
        reporter.scanCancelled(CancellationReason.CameraPermissionDenied)
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events.map { it["event"] }).containsExactly("cardscan_presented", "cardscan_cancel").inOrder()
        assertThat(events.first()["scan_session_id"]).isEqualTo(events.last()["scan_session_id"])
        assertThat(events.last()).doesNotContainKey("duration")
    }

    @Test
    fun `runtime failure before camera starts retains presented attempt`() = runScenario { reporter, executor ->
        reporter.scanPresented()
        reporter.scanFailed(IllegalStateException("runtime unavailable"))
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events.map { it["event"] }).containsExactly("cardscan_presented", "cardscan_failed").inOrder()
        assertThat(events.first()["scan_session_id"]).isEqualTo(events.last()["scan_session_id"])
        assertThat(events.last()).doesNotContainKey("duration")
    }

    @Test
    fun `presentation and camera milestones share one attempt`() = runScenario { reporter, executor ->
        reporter.scanPresented()
        reporter.scanPresented()
        ShadowSystemClock.advanceBy(10, TimeUnit.SECONDS)
        reporter.scanStarted()
        ShadowSystemClock.advanceBy(2, TimeUnit.SECONDS)
        reporter.scanDarkniteFoundPan()
        reporter.scanSucceeded()
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events).hasSize(4)
        events.forEach { assertThat(it["scan_session_id"]).isEqualTo(events.first()["scan_session_id"]) }
        assertThat(events.last()["duration"]).isEqualTo(2f)
    }

    @Test
    fun `late success and camera callbacks cannot reopen canceled attempt`() = runScenario { reporter, executor ->
        reporter.scanPresented()
        reporter.scanStarted()
        reporter.scanCancelled(CancellationReason.Back)
        reporter.scanSucceeded()
        reporter.scanFailed(null)
        reporter.scanStarted()
        reporter.scanDarkniteFoundPan()
        val events = executor.getExecutedRequests().map { it.params }
        assertThat(events.map { it["event"] })
            .containsExactly("cardscan_presented", "cardscan_scan_started", "cardscan_cancel").inOrder()
    }

    @Test
    fun `permission cancellation cannot end another reporters timer`() = runScenario { reporter, executor ->
        val durationProvider = DefaultDurationProvider.instance
        durationProvider.start(DurationProvider.Key.CardScan)
        reporter.scanPresented()
        reporter.scanCancelled(CancellationReason.CameraPermissionDenied)
        assertThat(executor.getExecutedRequests().last().params).doesNotContainKey("duration")
        assertThat(durationProvider.elapsed(DurationProvider.Key.CardScan)).isNotNull()
        durationProvider.end(DurationProvider.Key.CardScan)
    }

    private fun runScenario(
        configuration: CardScanConfiguration = CardScanConfiguration(ELEMENTS_SESSION_ID),
        testBlock: (DefaultCardScanEventsReporter, FakeAnalyticsRequestExecutor) -> Unit
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = DefaultDurationProvider.instance
        // Clear any leftover state from prior tests sharing the singleton.
        durationProvider.end(DurationProvider.Key.CardScan)
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
            durationProvider = durationProvider,
            cardScanConfiguration = configuration
        )

        testBlock(eventsReporter, analyticsRequestExecutor)
    }

    companion object {
        private const val ELEMENTS_SESSION_ID = "elements_session_id"
    }
}
