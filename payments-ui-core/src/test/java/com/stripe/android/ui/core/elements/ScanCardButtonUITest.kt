package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ScanCardButtonUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ScanCardButtonUI should launch legacy launcher when stripe card scan is available`() = runScenario(
        isStripeCardScanAvailable = true,
        isCardScanGooglePayMigrationEnabled = false,
    ) {
        composeTestRule.onNodeWithText("Scan card").performClick()
        assertThat(cardScanCall.awaitItem()).isEqualTo("stripe")
    }

    @Test
    fun `ScanCardButtonUI should launch Google launcher when Google migration is enabled and available`() = runScenario(
        isStripeCardScanAvailable = true,
        isCardScanGooglePayMigrationEnabled = true,
        isFetchClientSucceed = true,
    ) {
        composeTestRule.onNodeWithText("Scan card").performClick()
        assertThat(cardScanCall.awaitItem()).isEqualTo("google_pay")
    }

    @Test
    fun `ScanCardButtonUI hidden when Google migration is enabled but not available`() = runScenario(
        isStripeCardScanAvailable = true,
        isCardScanGooglePayMigrationEnabled = true,
        isFetchClientSucceed = false,
    ) {
        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
    }

    @Test
    fun `ScanCardButtonUI hidden when card scan is disabled`() = runScenario(
        isStripeCardScanAvailable = false,
        isCardScanGooglePayMigrationEnabled = false,
    ) {
        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
    }

    // Create mock controller for visibility tests
    private fun createMockController(
        cardScanCall: Turbine<String>,
        isStripeCardScanAvailable: Boolean
    ): CardDetailsSectionController {
        val controller = mock<CardDetailsSectionController>()
        val mockIsStripeCardScanAvailable = mock<DefaultIsStripeCardScanAvailable>()
        val mockCardDetailsElement = mock<CardDetailsElement>()
        val mockCardDetailsController = mock<CardDetailsController>()
        val mockNumberElement = mock<CardNumberElement>()
        val mockNumberElementController = mock<CardNumberController>()

        whenever(controller.isCardScanEnabled).thenReturn(true)

        whenever(controller.isStripeCardScanAvailable).thenReturn(mockIsStripeCardScanAvailable)
        whenever(mockIsStripeCardScanAvailable.invoke()).thenReturn(isStripeCardScanAvailable)

        whenever(controller.elementsSessionId).thenReturn("test_session")

        whenever(controller.cardDetailsElement).thenReturn(mockCardDetailsElement)
        whenever(mockCardDetailsElement.controller).thenReturn(mockCardDetailsController)

        whenever(mockCardDetailsController.numberElement).thenReturn(mockNumberElement)
        whenever(mockNumberElement.controller).thenReturn(mockNumberElementController)

        // Mock the onCardScanResult function that Google launcher needs
        whenever(mockCardDetailsController.onCardScanResult).thenReturn { cardScanCall.add("google_pay") }
        whenever(mockNumberElementController.onCardScanResult(any())).then { cardScanCall.add("stripe") }

        return controller
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
    )

    private fun runScenario(
        isStripeCardScanAvailable: Boolean = true,
        isCardScanGooglePayMigrationEnabled: Boolean = true,
        isFetchClientSucceed: Boolean = true,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(isCardScanGooglePayMigrationEnabled)

        val cardScanCall = Turbine<String>()
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
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
                LocalCardScanEventsReporter provides FakeCardScanEventsReporter(),
                LocalPaymentCardRecognitionClient provides FakePaymentCardRecognitionClient(isFetchClientSucceed)
            ) {
                ScanCardButtonUI(
                    enabled = true,
                    controller = createMockController(
                        cardScanCall = cardScanCall,
                        isStripeCardScanAvailable = isStripeCardScanAvailable
                    )
                )
            }
        }
        scenario.block()

        cardScanCall.ensureAllEventsConsumed()
    }
}
