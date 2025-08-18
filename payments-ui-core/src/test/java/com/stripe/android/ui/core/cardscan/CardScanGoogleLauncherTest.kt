package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.cardscan.FakePaymentCardRecognitionClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardScanGoogleLauncherTest {

    @Test
    fun `parseActivityResult with RESULT_CANCELED returns Canceled`() = runScenario {
        val result = ActivityResult(Activity.RESULT_CANCELED, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.available).isEqualTo(true)
        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")
    }

    @Test
    fun `parseActivityResult with RESULT_OK but null data returns Canceled`() = runScenario {
        val result = ActivityResult(Activity.RESULT_OK, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.available).isEqualTo(true)
        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")
    }

    @Test
    fun `parseActivityResult with RESULT_OK and valid data but no PAN returns Failed`() = runScenario {
        // Create an intent without PaymentCardRecognitionResult data
        val intent = Intent().apply {
            // Don't put any PaymentCardRecognitionResult data
        }
        val result = ActivityResult(Activity.RESULT_OK, intent)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Failed::class.java)
        val failedResult = scanResult as CardScanResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Failed to parse card data")

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.available).isEqualTo(true)
        val call = fakeEventsReporter.scanFailedCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")
        assertThat(call.error?.message).isEqualTo("Failed to parse card data")
    }

    @Test
    fun `parseActivityResult with custom result code returns Canceled`() = runScenario {
        val result = ActivityResult(123, null) // Some other result code

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.available).isEqualTo(true)
        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")
    }

    @Test
    fun `launch calls scanStarted event`() = runScenario {
        launcher.launch(ApplicationProvider.getApplicationContext())

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.available).isEqualTo(true)
        assertThat(fakeEventsReporter.scanStartedCalls.awaitItem().implementation).isEqualTo("google_pay")
        assertThat(activityLauncher.launchCall.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `init sets isAvailable to false and calls apiCheck on failure`() = runScenario(false) {
        assertThat(launcher.isAvailable.value).isFalse()

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.implementation).isEqualTo("google_pay")
        assertThat(apiCheckCall.available).isFalse()
        assertThat(apiCheckCall.reason).isNotNull() // Will contain Google Play Services error
    }

    @Test
    fun `init sets isAvailable to true and calls apiCheck on success`() = runScenario {
        assertThat(launcher.isAvailable.value).isTrue()

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.implementation).isEqualTo("google_pay")
        assertThat(apiCheckCall.available).isTrue()
        assertThat(apiCheckCall.reason).isNull() // No error reason for success
    }

    private class FakeCardScanEventsReporter : CardScanEventsReporter {
        data class ScanCancelledCall(val implementation: String)
        data class ScanFailedCall(val implementation: String, val error: Throwable?)
        data class ScanSucceededCall(val implementation: String)
        data class ScanStartedCall(val implementation: String)
        data class ApiCheckCall(val implementation: String, val available: Boolean, val reason: String?)

        private val _scanCancelledCalls = Turbine<ScanCancelledCall>()
        val scanCancelledCalls: ReceiveTurbine<ScanCancelledCall> = _scanCancelledCalls

        private val _scanFailedCalls = Turbine<ScanFailedCall>()
        val scanFailedCalls: ReceiveTurbine<ScanFailedCall> = _scanFailedCalls

        private val _scanSucceededCalls = Turbine<ScanSucceededCall>()
        val scanSucceededCalls: ReceiveTurbine<ScanSucceededCall> = _scanSucceededCalls

        private val _scanStartedCalls = Turbine<ScanStartedCall>()
        val scanStartedCalls: ReceiveTurbine<ScanStartedCall> = _scanStartedCalls

        private val _apiCheckCalls = Turbine<ApiCheckCall>()
        val apiCheckCalls: ReceiveTurbine<ApiCheckCall> = _apiCheckCalls

        fun validate() {
            _scanCancelledCalls.ensureAllEventsConsumed()
            _scanFailedCalls.ensureAllEventsConsumed()
            _scanSucceededCalls.ensureAllEventsConsumed()
            _scanStartedCalls.ensureAllEventsConsumed()
            _apiCheckCalls.ensureAllEventsConsumed()
        }

        override fun onCardScanCancelled(implementation: String) {
            _scanCancelledCalls.add(ScanCancelledCall(implementation))
        }

        override fun onCardScanFailed(implementation: String, error: Throwable?) {
            _scanFailedCalls.add(ScanFailedCall(implementation, error))
        }

        override fun onCardScanSucceeded(implementation: String) {
            _scanSucceededCalls.add(ScanSucceededCall(implementation))
        }

        override fun onCardScanStarted(implementation: String) {
            _scanStartedCalls.add(ScanStartedCall(implementation))
        }

        override fun onCardScanApiCheck(implementation: String, available: Boolean, reason: String?) {
            _apiCheckCalls.add(ApiCheckCall(implementation, available, reason))
        }
    }

    private class FakeActivityLauncher<I> : ActivityResultLauncher<I>() {
        private val _launchCall = Turbine<Unit>()
        val launchCall: ReceiveTurbine<Unit> = _launchCall
        override val contract: ActivityResultContract<I, *>
            get() = throw NotImplementedError("Not implemented!")

        override fun launch(input: I, options: ActivityOptionsCompat?) {
            _launchCall.add(Unit)
        }

        override fun unregister() {
            throw NotImplementedError("Not implemented!")
        }

        fun validate() {
            _launchCall.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val launcher: CardScanGoogleLauncher,
        val fakeEventsReporter: FakeCardScanEventsReporter,
        val activityLauncher: FakeActivityLauncher<IntentSenderRequest>,
    )

    private fun runScenario(
        isFetchClientSuccess: Boolean = true,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val fakeEventsReporter = FakeCardScanEventsReporter()
        val activityLauncher = FakeActivityLauncher<IntentSenderRequest>()
        val launcher = CardScanGoogleLauncher(
            context = ApplicationProvider.getApplicationContext(),
            eventsReporter = fakeEventsReporter,
            paymentCardRecognitionClient = FakePaymentCardRecognitionClient(isFetchClientSuccess)
        ).apply {
            this.activityLauncher = activityLauncher
        }

        val scenario = Scenario(
            launcher = launcher,
            fakeEventsReporter = fakeEventsReporter,
            activityLauncher = activityLauncher
        )

        scenario.block()

        fakeEventsReporter.validate()
        activityLauncher.validate()
    }
}
