package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CardScanActionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `automatically launches card scan and gets result`() {
        val helper = AutomaticallyLaunchedCardScanFormDataHelper(
            openCardScanAutomaticallyConfig = true,
            hasAutomaticallyLaunchedCardScanInitialValue = false,
            savedStateHandle = SavedStateHandle()
        )
        runScenario(helper = helper) {
            assertThat(onScannedCardCalls.awaitItem().scannedCardDetails).isEqualTo(
                ScannedCardDetails.Unvalidated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2042,
                    expirationMonth = 2,
                )
            )
            assertThat(helper.hasAutomaticallyLaunchedCardScan).isTrue()
            assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
        }
    }

    @Test
    fun `does not launch card scan when openCardScanAutomaticallyConfig false`() {
        val helper = AutomaticallyLaunchedCardScanFormDataHelper(
            openCardScanAutomaticallyConfig = false,
            hasAutomaticallyLaunchedCardScanInitialValue = false,
            savedStateHandle = SavedStateHandle()
        )
        runScenario(helper = helper) {
            onScannedCardCalls.expectNoEvents()
            assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
        }
    }

    @Test
    fun `does not launch card scan when hasAutomaticallyLaunchedCardScanInitialValue true`() {
        val helper = AutomaticallyLaunchedCardScanFormDataHelper(
            openCardScanAutomaticallyConfig = true,
            hasAutomaticallyLaunchedCardScanInitialValue = true,
            savedStateHandle = SavedStateHandle()
        )
        runScenario(helper = helper) {
            onScannedCardCalls.expectNoEvents()
            assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
        }
    }

    @Test
    fun `does not launch card scan when automaticallyLaunchedCardScanFormDataHelper null`() {
        runScenario(helper = null) {
            onScannedCardCalls.expectNoEvents()
        }
    }

    private class Scenario(
        val onScannedCardCalls: ReceiveTurbine<OnScannedCardCall>,
    )

    private data class OnScannedCardCall(
        val scannedCardDetails: ScannedCardDetails,
    )

    private fun runScenario(
        helper: AutomaticallyLaunchedCardScanFormDataHelper?,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val mockResult = mock<PaymentCardRecognitionResult>()
        val mockExpirationDate = mock<CreditCardExpirationDate>()
        whenever(mockResult.pan).thenReturn("4242424242424242")
        whenever(mockResult.creditCardExpirationDate).thenReturn(mockExpirationDate)
        whenever(mockExpirationDate.month).thenReturn(2)
        whenever(mockExpirationDate.year).thenReturn(2042)
        val intent = Intent().putExtra(
            "com.google.android.gms.wallet.PaymentCardRecognitionResult",
            mockResult
        )
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I : Any?, O : Any?> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?
                    ) {
                        this.dispatchResult(requestCode, Activity.RESULT_OK, intent)
                    }
                }
        }

        val onScannedCardCalls = Turbine<OnScannedCardCall>()
        val onScannedCard = { scannedCardDetails: ScannedCardDetails ->
            onScannedCardCalls.add(OnScannedCardCall(scannedCardDetails))
        }

        val action = CardScanAction(
            isStripeCardScanAllowed = false,
            enableMlKitCardScan = false,
            disableSsdOcrCardScan = false,
            automaticallyLaunchedCardScanFormDataHelper = helper,
        )
        val scenario = Scenario(onScannedCardCalls = onScannedCardCalls)

        mockStatic(PaymentCardRecognitionResult::class.java).use { mockedStatic ->
            mockedStatic.`when`<PaymentCardRecognitionResult> {
                PaymentCardRecognitionResult.getFromIntent(any())
            }.thenReturn(mockResult)
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides registryOwner,
                    LocalCardScanEventsReporter provides FakeCardScanEventsReporter(),
                    LocalPaymentCardRecognitionClient provides FakePaymentCardRecognitionClient(true)
                ) {
                    action.Content(enabled = true, onScannedCard = onScannedCard)
                }
            }
        }

        scenario.block()
    }
}
