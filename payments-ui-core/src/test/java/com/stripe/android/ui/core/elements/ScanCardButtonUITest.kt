package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.cardscan.CardScanGoogleLauncher.Companion.rememberCardScanGoogleLauncher
import com.stripe.android.ui.core.cardscan.CardScanLauncher
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ScanCardButtonUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `ScanCardButtonUI should launch Google launcher when GPCR is available`() = runScenario {
        scanButtonShownCall.awaitItem()
        composeTestRule.onNodeWithText("Scan card").performClick()
        assertThat(cardScanCall.awaitItem()).isEqualTo("google_pay")
    }

    @Test
    fun `ScanCardButtonUI fires button shown event when visible`() = runScenario {
        composeTestRule.onNodeWithText("Scan card").assertExists()
        assertThat(scanButtonShownCall.awaitItem())
            .isEqualTo(FakeCardScanEventsReporter.ScanButtonShownCall)
    }

    private fun createMockPaymentCardRecognitionResultIntent(): Intent {
        val mockResult = Mockito.mock<PaymentCardRecognitionResult>()
        val mockExpirationDate = Mockito.mock<CreditCardExpirationDate>()

        whenever(mockResult.pan).thenReturn("4242424242424242")
        whenever(mockResult.creditCardExpirationDate).thenReturn(mockExpirationDate)
        whenever(mockExpirationDate.month).thenReturn(12)
        whenever(mockExpirationDate.year).thenReturn(2042)

        val intent = Intent().putExtra(
            "com.google.android.gms.wallet.PaymentCardRecognitionResult",
            mockResult
        )

        return intent
    }
    private class Scenario(
        val cardScanCall: ReceiveTurbine<String>,
        val scanButtonShownCall: ReceiveTurbine<FakeCardScanEventsReporter.ScanButtonShownCall>,
    )

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val cardScanCall = Turbine<String>()
        val fakeEventsReporter = FakeCardScanEventsReporter()
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I : Any?, O : Any?> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?
                    ) {
                        this.dispatchResult(
                            requestCode,
                            Activity.RESULT_OK,
                            createMockPaymentCardRecognitionResultIntent()
                        )
                    }
                }
        }

        val scenario = Scenario(
            cardScanCall = cardScanCall,
            scanButtonShownCall = fakeEventsReporter.scanButtonShownCalls,
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
                LocalCardScanEventsReporter provides fakeEventsReporter,
                LocalPaymentCardRecognitionClient provides FakePaymentCardRecognitionClient(shouldSucceed = true)
            ) {
                val context = LocalContext.current
                val eventsReporter = LocalCardScanEventsReporter.current
                val cardScanLauncher: CardScanLauncher = rememberCardScanGoogleLauncher(
                    context = context,
                    eventsReporter = eventsReporter,
                ) { cardScanCall.add("google_pay") }

                ScanCardButtonUI(
                    enabled = true,
                    cardScanLauncher = cardScanLauncher,
                )
            }
        }
        scenario.block()

        cardScanCall.ensureAllEventsConsumed()
    }
}
