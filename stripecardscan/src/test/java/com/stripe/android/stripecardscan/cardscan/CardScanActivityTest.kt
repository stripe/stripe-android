package com.stripe.android.stripecardscan.cardscan

import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardScanActivityTest {
    @Test
    fun `scanSucceeded event is sent on scan completion`() = runTest {
        FakeCardScansEventReporter.test {
            val scenario = ActivityScenario.launch(CardScanActivity::class.java)

            scenario.onActivity { activity ->
                activity.cardScanEventsReporter = eventsReporter
                activity.resultListener.cardScanComplete(ScannedCard(""))
            }

            awaitScanSucceeded()
        }
    }

    @Test
    fun `scanFailed event is sent on scan failed`() = runTest {
        FakeCardScansEventReporter.test {
            val error = Throwable("oops")
            val scenario = ActivityScenario.launch(CardScanActivity::class.java)

            scenario.onActivity { activity ->
                activity.cardScanEventsReporter = eventsReporter
                activity.resultListener.failed(error)
            }

            assertThat(awaitScanFailed()).isEqualTo(error)
        }
    }

    @Test
    fun `scanCancelled event is sent on cancellation`() = runTest {
        FakeCardScansEventReporter.test {
            val scenario = ActivityScenario.launch(CardScanActivity::class.java)

            scenario.onActivity { activity ->
                activity.cardScanEventsReporter = eventsReporter
                activity.resultListener.userCanceled(CancellationReason.Back)
            }

            awaitScanCancelled()
        }
    }

    @Test
    fun `scanStarted event is sent on scan started`() = runTest {
        FakeCardScansEventReporter.test {
            val scenario = ActivityScenario.launch(CardScanActivity::class.java)

            scenario.onActivity { activity ->
                activity.cardScanEventsReporter = eventsReporter
                this@runTest.launch {
                    activity.onCameraStreamAvailable(flowOf())
                }
            }

            awaitScanStarted()
        }
    }
}
