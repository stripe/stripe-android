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
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardScanGoogleLauncherTest {

    @Test
    fun `parseActivityResult with valid GPCR data returns Completed`() = runScenario {
        val mockResult = mock<PaymentCardRecognitionResult>()
        val mockExpirationDate = mock<CreditCardExpirationDate>()

        whenever(mockResult.pan).thenReturn("4242424242424242")
        whenever(mockResult.creditCardExpirationDate).thenReturn(mockExpirationDate)
        whenever(mockExpirationDate.month).thenReturn(12)
        whenever(mockExpirationDate.year).thenReturn(2042)

        mockStatic(PaymentCardRecognitionResult::class.java).use { mockedStatic ->
            mockedStatic.`when`<PaymentCardRecognitionResult> {
                PaymentCardRecognitionResult.getFromIntent(any())
            }.thenReturn(mockResult)

            val intent = Intent().putExtra(
                "com.google.android.gms.wallet.PaymentCardRecognitionResult",
                mockResult
            )
            val result = ActivityResult(Activity.RESULT_OK, intent)

            val scanResult = launcher.parseActivityResult(result)

            assertThat(scanResult).isInstanceOf(CardScanResult.Completed::class.java)
            val completedResult = scanResult as CardScanResult.Completed
            assertThat(completedResult.scannedCard.pan).isEqualTo("4242424242424242")
            assertThat(completedResult.scannedCard.expirationMonth).isEqualTo(12)
            assertThat(completedResult.scannedCard.expirationYear).isEqualTo(2042)
        }
    }

    @Test
    fun `parseActivityResult with RESULT_CANCELED returns Canceled`() = runScenario {
        val result = ActivityResult(Activity.RESULT_CANCELED, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Canceled::class.java)
    }

    @Test
    fun `parseActivityResult with RESULT_OK but null data returns Failed`() = runScenario {
        val result = ActivityResult(Activity.RESULT_OK, null)

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Failed::class.java)
        val failedResult = scanResult as CardScanResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Null data or unexpected result code: -1")
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
    }

    @Test
    fun `parseActivityResult with custom result code returns Failed`() = runScenario {
        val result = ActivityResult(123, null) // Some other result code

        val scanResult = launcher.parseActivityResult(result)

        assertThat(scanResult).isInstanceOf(CardScanResult.Failed::class.java)
        val failedResult = scanResult as CardScanResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Null data or unexpected result code: 123")
    }

    @Test
    fun `card scan launcher should be able to launch card scan activity`() = runScenario {
        assertThat(launcher.isAvailable.value).isTrue()

        launcher.launch(ApplicationProvider.getApplicationContext())
        assertThat(activityLauncher.launchCall.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `card scan launcher should not be available when fetchIntent fails`() = runScenario(
        isFetchClientSucceed = false
    ) {
        assertThat(launcher.isAvailable.value).isFalse()
        launcher.launch(ApplicationProvider.getApplicationContext())
        // No launch call should be made since fetchIntent failed
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
        val activityLauncher: FakeActivityLauncher<IntentSenderRequest>,
    )

    private fun runScenario(
        isFetchClientSucceed: Boolean = true,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val activityLauncher = FakeActivityLauncher<IntentSenderRequest>()
        val launcher = CardScanGoogleLauncher(
            context = ApplicationProvider.getApplicationContext(),
            eventsReporter = FakeCardScanEventsReporter(),
            paymentCardRecognitionClient = FakePaymentCardRecognitionClient(isFetchClientSucceed)
        ).apply {
            this.activityLauncher = activityLauncher
        }

        val scenario = Scenario(
            launcher = launcher,
            activityLauncher = activityLauncher
        )

        scenario.block()

        activityLauncher.validate()
    }
}
