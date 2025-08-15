package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
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
class CardScanGoogleLauncherParseResultTest {

    private val fakeEventsReporter = FakeCardScanEventsReporter()
    private val launcher = CardScanGoogleLauncher(
        context = ApplicationProvider.getApplicationContext(),
        eventsReporter = fakeEventsReporter
    )

    @Test
    fun `parseActivityResult with RESULT_CANCELED returns Canceled`() = runTest {
        val result = ActivityResult(Activity.RESULT_CANCELED, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")

        fakeEventsReporter.validate()
    }

    @Test
    fun `parseActivityResult with RESULT_OK but null data returns Canceled`() = runTest {
        val result = ActivityResult(Activity.RESULT_OK, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")

        fakeEventsReporter.validate()
    }

    @Test
    fun `parseActivityResult with RESULT_OK and valid data but no PAN returns Failed`() = runTest {
        // Create an intent without PaymentCardRecognitionResult data
        val intent = Intent().apply {
            // Don't put any PaymentCardRecognitionResult data
        }
        val result = ActivityResult(Activity.RESULT_OK, intent)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Failed::class.java)
        val failedResult = scanResult as CardScanResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Failed to parse card data")

        val call = fakeEventsReporter.scanFailedCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")
        assertThat(call.error?.message).isEqualTo("Failed to parse card data")

        fakeEventsReporter.validate()
    }

    @Test
    fun `parseActivityResult with custom result code returns Canceled`() = runTest {
        val result = ActivityResult(123, null) // Some other result code

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)

        val call = fakeEventsReporter.scanCancelledCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")

        fakeEventsReporter.validate()
    }

    @Test
    fun `launch calls scanStarted event`() = runTest {
        launcher.launch(ApplicationProvider.getApplicationContext())

        val call = fakeEventsReporter.scanStartedCalls.awaitItem()
        assertThat(call.implementation).isEqualTo("google_pay")

        // Note: We can't test activityLauncher.launch() directly because it's lateinit
        // and requires Compose initialization, but we can verify the observable behavior
        fakeEventsReporter.validate()
    }

    @Test
    fun `init sets isAvailable to false and calls apiCheck on failure`() = runTest {
        // Create launcher in test - this will trigger the init block
        val localLauncher = CardScanGoogleLauncher(
            context = ApplicationProvider.getApplicationContext(),
            eventsReporter = fakeEventsReporter,
            paymentCardRecognitionClient = FakePaymentCardRecognitionClient(false)
        )

        // Check that isAvailable is false (due to Google Play Services not being available in tests)
        assertThat(localLauncher.isAvailable.value).isFalse()

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.implementation).isEqualTo("google_pay")
        assertThat(apiCheckCall.available).isFalse()
        assertThat(apiCheckCall.reason).isNotNull() // Will contain Google Play Services error

        fakeEventsReporter.validate()
    }

    @Test
    fun `init sets isAvailable to true and calls apiCheck on success`() = runTest {
        // Create launcher with successful fake client - this will trigger the init block
        val localLauncher = CardScanGoogleLauncher(
            context = ApplicationProvider.getApplicationContext(),
            eventsReporter = fakeEventsReporter,
            paymentCardRecognitionClient = FakePaymentCardRecognitionClient(true)
        )

        // Check that isAvailable is true (success case)
        assertThat(localLauncher.isAvailable.value).isTrue()

        val apiCheckCall = fakeEventsReporter.apiCheckCalls.awaitItem()
        assertThat(apiCheckCall.implementation).isEqualTo("google_pay")
        assertThat(apiCheckCall.available).isTrue()
        assertThat(apiCheckCall.reason).isNull() // No error reason for success

        fakeEventsReporter.validate()
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

        override fun scanCancelled(implementation: String) {
            _scanCancelledCalls.add(ScanCancelledCall(implementation))
        }

        override fun scanFailed(implementation: String, error: Throwable?) {
            _scanFailedCalls.add(ScanFailedCall(implementation, error))
        }

        override fun scanSucceeded(implementation: String) {
            _scanSucceededCalls.add(ScanSucceededCall(implementation))
        }

        override fun scanStarted(implementation: String) {
            _scanStartedCalls.add(ScanStartedCall(implementation))
        }

        override fun apiCheck(implementation: String, available: Boolean, reason: String?) {
            _apiCheckCalls.add(ApiCheckCall(implementation, available, reason))
        }
    }
}
