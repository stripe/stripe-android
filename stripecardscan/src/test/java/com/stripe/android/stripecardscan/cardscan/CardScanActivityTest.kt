package com.stripe.android.stripecardscan.cardscan

import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardScanActivityTest {
    @Test
    fun `scanSucceeded event is sent on scan completion`() = testScenario {
        activityScenario.onActivity { activity ->
            activity.cardScanEventsReporter = eventsReporter
            activity.resultListener.cardScanComplete(ScannedCard(""))
        }

        eventReporterScenario.awaitScanSucceeded()
    }

    @Test
    fun `scanFailed event is sent on scan failed`() = testScenario {
        val error = Throwable("oops")

        activityScenario.onActivity { activity ->
            activity.cardScanEventsReporter = eventsReporter
            activity.resultListener.failed(error)
        }

        assertThat(eventReporterScenario.awaitScanFailed()).isEqualTo(error)
    }

    @Test
    fun `scanCancelled event is sent on cancellation`() = testScenario {
        activityScenario.onActivity { activity ->
            activity.cardScanEventsReporter = eventsReporter
            activity.resultListener.userCanceled(CancellationReason.Back)
        }

        eventReporterScenario.awaitScanCancelled()
    }

    @Test
    fun `scanStarted event is sent on scan started`() = testScenario {
        activityScenario.onActivity { activity ->
            activity.cardScanEventsReporter = eventsReporter
            scope.launch {
                activity.onCameraStreamAvailable(flowOf())
            }
        }

        eventReporterScenario.awaitScanStarted()
    }

    private fun testScenario(test: suspend TestParams.() -> Unit) = runTest {
        FakeCardScansEventReporter.test {
            val activityScenario = ActivityScenario.launch(CardScanActivity::class.java)
            val params = TestParams(
                activityScenario = activityScenario,
                eventReporterScenario = this,
                scope = this@runTest
            )
            test(params)
        }
    }

    private data class TestParams(
        val activityScenario: ActivityScenario<CardScanActivity>,
        val eventReporterScenario: FakeCardScansEventReporter.Scenario,
        val scope: TestScope
    ) {
        val eventsReporter = eventReporterScenario.eventsReporter
    }
}
