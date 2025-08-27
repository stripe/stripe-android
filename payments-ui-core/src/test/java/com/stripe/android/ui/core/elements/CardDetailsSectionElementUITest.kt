package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.stripecardscan.R
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.ScannedCard
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CardDetailsSectionElementUITest {
    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeCardScanDefaultTheme)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `CardDetailsSectionElement automatically launches card scan and gets result`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        runScenario(
            automaticallyLaunchedCardScanFormDataHelper = AutomaticallyLaunchedCardScanFormDataHelper(
                openCardScanAutomaticallyConfig = true,
                hasAutomaticallyLaunchedCardScanInitialValue = false,
                savedStateHandle = SavedStateHandle()
            )
        ) {
            composeTestRule.onNodeWithText("4242 4242 4242 4242").assertExists()

            verify(controller, times(1)).onCardScanResult(
                CardScanResult.Completed(
                    scannedCard = ScannedCard(
                        pan = "4242424242424242",
                        expirationYear = 2042,
                        expirationMonth = 2,
                    )
                )
            )
            verify(controller, times(1)).setHasAutomaticallyLaunchedCardScan()

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    @Test
    fun `CardDetailsSectionElement does not launch card scan when openCardScanAutomaticallyConfig false`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        runScenario(
            automaticallyLaunchedCardScanFormDataHelper = AutomaticallyLaunchedCardScanFormDataHelper(
                openCardScanAutomaticallyConfig = false,
                hasAutomaticallyLaunchedCardScanInitialValue = false,
                savedStateHandle = SavedStateHandle()
            )
        ) {
            composeTestRule.onNodeWithText("4242 4242 4242 4242").assertDoesNotExist()
            composeTestRule.onNodeWithText("Card number").assertExists()

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()

            verify(controller, times(0)).onCardScanResult(any())
            verify(controller, times(0)).setHasAutomaticallyLaunchedCardScan()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    @Test
    fun `CardDetailsSectionElement does not launch card scan when hasAutomaticallyLaunchedCardScanInitialValue true`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)

        runScenario(
            automaticallyLaunchedCardScanFormDataHelper = AutomaticallyLaunchedCardScanFormDataHelper(
                openCardScanAutomaticallyConfig = true,
                hasAutomaticallyLaunchedCardScanInitialValue = true,
                savedStateHandle = SavedStateHandle()
            )
        ) {
            composeTestRule.onNodeWithText("4242 4242 4242 4242").assertDoesNotExist()
            composeTestRule.onNodeWithText("Card number").assertExists()

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()
            verify(controller, times(0)).onCardScanResult(any())
            verify(controller, times(0)).setHasAutomaticallyLaunchedCardScan()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    @Test
    fun `CardDetailsSectionElement does not launch card scan when FeatureFlags cardScanGooglePayMigration disabled`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
        runScenario(
            automaticallyLaunchedCardScanFormDataHelper = AutomaticallyLaunchedCardScanFormDataHelper(
                openCardScanAutomaticallyConfig = true,
                hasAutomaticallyLaunchedCardScanInitialValue = false,
                savedStateHandle = SavedStateHandle()
            )
        ) {
            composeTestRule.onNodeWithText("4242 4242 4242 4242").assertDoesNotExist()
            composeTestRule.onNodeWithText("Card number").assertExists()

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()
            verify(controller, times(0)).onCardScanResult(any())
            verify(controller, times(0)).setHasAutomaticallyLaunchedCardScan()
        }
    }

    @Test
    fun `CardDetailsSectionElement does not launch card scan when automaticallyLaunchedCardScanFormData null`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        runScenario(
            automaticallyLaunchedCardScanFormDataHelper = null
        ) {
            composeTestRule.onNodeWithText("4242 4242 4242 4242").assertDoesNotExist()
            composeTestRule.onNodeWithText("Card number").assertExists()

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()
            verify(controller, times(0)).onCardScanResult(any())
            verify(controller, times(0)).setHasAutomaticallyLaunchedCardScan()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    private class Scenario(
        val controller: CardDetailsSectionController,
    )

    private fun getController(
        context: Context,
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
    ): CardDetailsSectionController {
        val cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context)

        val output = CardDetailsSectionController(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            initialValues = emptyMap(),
            collectName = false,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            cardBrandFilter = DefaultCardBrandFilter,
            elementsSessionId = "test_session",
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper
        )
        return output
    }

    private fun runScenario(
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
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
                        this.dispatchResult(
                            requestCode,
                            Activity.RESULT_OK,
                            intent
                        )
                    }
                }
        }
        val controller = getController(
            context = context,
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper
        )

        val cardDetailsSectionControllerSpy = spy(controller)
        val scenario = Scenario(
            controller = cardDetailsSectionControllerSpy,
        )

        mockStatic(PaymentCardRecognitionResult::class.java).use { mockedStatic ->
            mockedStatic.`when`<PaymentCardRecognitionResult> {
                PaymentCardRecognitionResult.getFromIntent(any())
            }.thenReturn(mockResult)
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides registryOwner,
                    LocalCardNumberCompletedEventReporter provides { },
                    LocalCardScanEventsReporter provides FakeCardScanEventsReporter(),
                    LocalPaymentCardRecognitionClient provides FakePaymentCardRecognitionClient(true)
                ) {
                    CardDetailsSectionElementUI(
                        enabled = true,
                        controller = cardDetailsSectionControllerSpy,
                        hiddenIdentifiers = emptySet(),
                        lastTextFieldIdentifier = IdentifierSpec.PostalCode
                    )
                }
            }
        }

        scenario.block()
    }
}
