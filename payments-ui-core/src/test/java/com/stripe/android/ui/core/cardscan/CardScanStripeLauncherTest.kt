package com.stripe.android.ui.core.cardscan

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.scanui.CancellationReason
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.stripecardscan.payment.card.ScannedCard as StripeScannedCard

@RunWith(RobolectricTestRunner::class)
class CardScanStripeLauncherTest {

    @Test
    fun `parseActivityResult with Completed result returns Completed with PAN only`() = runScenario {
        val sheetResult = CardScanSheetResult.Completed(StripeScannedCard(pan = "4242424242424242"))
        val intent = Intent().putExtra("result", sheetResult)

        val result = launcher.parseActivityResult(intent)

        assertThat(result).isInstanceOf(CardScanResult.Completed::class.java)
        val completed = result as CardScanResult.Completed
        assertThat(completed.scannedCard.pan).isEqualTo("4242424242424242")
        assertThat(completed.scannedCard.expirationMonth).isNull()
        assertThat(completed.scannedCard.expirationYear).isNull()

        assertThat(fakeEventsReporter.scanSucceededCalls.awaitItem().implementation)
            .isEqualTo("stripe_card_scan")
    }

    @Test
    fun `parseActivityResult with Completed result includes expiry when present`() = runScenario {
        val sheetResult = CardScanSheetResult.Completed(
            StripeScannedCard(pan = "4242424242424242", expiryMonth = 12, expiryYear = 2028)
        )
        val intent = Intent().putExtra("result", sheetResult)

        val result = launcher.parseActivityResult(intent)

        assertThat(result).isInstanceOf(CardScanResult.Completed::class.java)
        val completed = result as CardScanResult.Completed
        assertThat(completed.scannedCard.pan).isEqualTo("4242424242424242")
        assertThat(completed.scannedCard.expirationMonth).isEqualTo(12)
        assertThat(completed.scannedCard.expirationYear).isEqualTo(2028)

        assertThat(fakeEventsReporter.scanSucceededCalls.awaitItem().implementation)
            .isEqualTo("stripe_card_scan")
    }

    @Test
    fun `parseActivityResult with Canceled result returns Canceled`() = runScenario {
        val sheetResult = CardScanSheetResult.Canceled(CancellationReason.Closed)
        val intent = Intent().putExtra("result", sheetResult)

        val result = launcher.parseActivityResult(intent)

        assertThat(result).isInstanceOf(CardScanResult.Canceled::class.java)

        assertThat(fakeEventsReporter.scanCancelledCalls.awaitItem().implementation)
            .isEqualTo("stripe_card_scan")
    }

    @Test
    fun `parseActivityResult with Failed result returns Failed`() = runScenario {
        val error = RuntimeException("Scan failed")
        val sheetResult = CardScanSheetResult.Failed(error)
        val intent = Intent().putExtra("result", sheetResult)

        val result = launcher.parseActivityResult(intent)

        assertThat(result).isInstanceOf(CardScanResult.Failed::class.java)
        val failed = result as CardScanResult.Failed
        assertThat(failed.error).isEqualTo(error)

        val scanFailedCall = fakeEventsReporter.scanFailedCalls.awaitItem()
        assertThat(scanFailedCall.implementation).isEqualTo("stripe_card_scan")
        assertThat(scanFailedCall.error).isEqualTo(error)
    }

    @Test
    fun `parseActivityResult with null intent returns Failed`() = runScenario {
        val result = launcher.parseActivityResult(null)

        assertThat(result).isInstanceOf(CardScanResult.Failed::class.java)
        val failed = result as CardScanResult.Failed
        assertThat(failed.error).isInstanceOf(UnknownScanException::class.java)

        val scanFailedCall = fakeEventsReporter.scanFailedCalls.awaitItem()
        assertThat(scanFailedCall.implementation).isEqualTo("stripe_card_scan")
    }

    private class Scenario(
        val launcher: CardScanStripeLauncher,
        val fakeEventsReporter: FakeCardScanEventsReporter,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val fakeEventsReporter = FakeCardScanEventsReporter()
        val launcher = CardScanStripeLauncher(
            context = ApplicationProvider.getApplicationContext(),
            eventsReporter = fakeEventsReporter,
            enableMlKitCardScan = false,
            elementsSessionId = null,
            isLaunchingState = mutableStateOf(false),
        )

        val scenario = Scenario(
            launcher = launcher,
            fakeEventsReporter = fakeEventsReporter,
        )

        scenario.block()

        fakeEventsReporter.validate()
    }
}
