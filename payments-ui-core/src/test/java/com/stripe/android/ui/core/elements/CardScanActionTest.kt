package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.cardscan.CardScanLoadingState
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.PaymentCardRecognitionClient
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `automatically launches card scan and gets result`() {
        val helper = AutomaticallyLaunchedCardScanFormDataHelper(
            openCardScanAutomaticallyConfig = true,
            hasAutomaticallyLaunchedCardScanInitialValue = false,
            savedStateHandle = SavedStateHandle()
        )
        runScenario(
            helper = helper,
            cardScanLoadingState = CardScanLoadingState.Available,
            backupAction = null,
        ) {
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
        runScenario(
            helper = helper,
            cardScanLoadingState = CardScanLoadingState.Available,
            backupAction = null,
        ) {
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
        runScenario(
            helper = helper,
            cardScanLoadingState = CardScanLoadingState.Available,
            backupAction = null,
        ) {
            onScannedCardCalls.expectNoEvents()
            assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
        }
    }

    @Test
    fun `does not launch card scan when automaticallyLaunchedCardScanFormDataHelper null`() {
        runScenario(
            helper = null,
            cardScanLoadingState = CardScanLoadingState.Available,
            backupAction = null,
        ) {
            onScannedCardCalls.expectNoEvents()
        }
    }

    @Test
    fun `shows backup action when card scan is unavailable`() = runScenario(
        helper = null,
        cardScanLoadingState = CardScanLoadingState.Unavailable,
        backupAction = FakeCardDetailsAction(contentText = "NFC scan"),
    ) {
        composeTestRule.onNodeWithText("NFC scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
        onScannedCardCalls.expectNoEvents()
    }

    @Test
    fun `does not fire button shown event when Google card scan is unavailable`() = runScenario(
        helper = null,
        cardScanLoadingState = CardScanLoadingState.Unavailable,
        backupAction = null,
    ) {
        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
        fakeEventsReporter.scanButtonShownCalls.expectNoEvents()
        onScannedCardCalls.expectNoEvents()
    }

    @Test
    fun `shows card scan action when card scan is available`() = runScenario(
        helper = null,
        cardScanLoadingState = CardScanLoadingState.Available,
        backupAction = FakeCardDetailsAction(contentText = "NFC scan"),
    ) {
        composeTestRule.onNodeWithText("Scan card").assertIsDisplayed()
        composeTestRule.onNodeWithText("NFC scan").assertDoesNotExist()
        assertThat(fakeEventsReporter.scanButtonShownCalls.awaitItem())
            .isEqualTo(FakeCardScanEventsReporter.ScanButtonShownCall)
        onScannedCardCalls.expectNoEvents()
    }

    @Test
    fun `shows no action while card scan is loading`() = runScenario(
        helper = null,
        cardScanLoadingState = CardScanLoadingState.Loading,
        backupAction = FakeCardDetailsAction(contentText = "NFC scan"),
    ) {
        composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
        composeTestRule.onNodeWithText("NFC scan").assertDoesNotExist()
        onScannedCardCalls.expectNoEvents()
    }

    @Test
    fun `shows Google card scan action when loading moves from loading to available`() {
        val paymentCardRecognitionClient = FakeLoadingPaymentCardRecognitionClient()

        runScenario(
            helper = null,
            cardScanLoadingState = CardScanLoadingState.Loading,
            backupAction = FakeCardDetailsAction(contentText = "NFC scan"),
            paymentCardRecognitionClient = paymentCardRecognitionClient,
        ) {
            composeTestRule.onNodeWithText("Scan card").assertDoesNotExist()
            composeTestRule.onNodeWithText("NFC scan").assertDoesNotExist()

            paymentCardRecognitionClient.fetchIntentCalls.awaitItem().onSuccess(
                mock<IntentSenderRequest>()
            )
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Scan card").assertIsDisplayed()
            composeTestRule.onNodeWithText("NFC scan").assertDoesNotExist()
            assertThat(fakeEventsReporter.scanButtonShownCalls.awaitItem())
                .isEqualTo(FakeCardScanEventsReporter.ScanButtonShownCall)
            onScannedCardCalls.expectNoEvents()
        }

        paymentCardRecognitionClient.fetchIntentCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val onScannedCardCalls: ReceiveTurbine<OnScannedCardCall>,
        val fakeEventsReporter: FakeCardScanEventsReporter,
    )

    private data class OnScannedCardCall(
        val scannedCardDetails: ScannedCardDetails,
    )

    private class FakeCardDetailsAction(
        private val contentText: String,
    ) : CardDetailsAction {
        @Composable
        override fun Content(enabled: Boolean, onScannedCard: (ScannedCardDetails) -> Unit) {
            Text(contentText)
        }
    }

    private class FakeLoadingPaymentCardRecognitionClient : PaymentCardRecognitionClient {
        val fetchIntentCalls = Turbine<FetchIntentCall>()

        override fun fetchIntent(
            context: Context,
            onFailure: (Throwable) -> Unit,
            onSuccess: (IntentSenderRequest) -> Unit,
        ) {
            fetchIntentCalls.add(
                FetchIntentCall(
                    context = context,
                    onFailure = onFailure,
                    onSuccess = onSuccess,
                )
            )
        }

        data class FetchIntentCall(
            val context: Context,
            val onFailure: (Throwable) -> Unit,
            val onSuccess: (IntentSenderRequest) -> Unit,
        )
    }

    private fun runScenario(
        helper: AutomaticallyLaunchedCardScanFormDataHelper?,
        cardScanLoadingState: CardScanLoadingState,
        backupAction: CardDetailsAction?,
        paymentCardRecognitionClient: PaymentCardRecognitionClient =
            FakePaymentCardRecognitionClient(cardScanLoadingState),
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
            apiConfiguration = API_CONFIGURATION,
            isStripeCardScanAllowed = false,
            enableMlKitCardScan = false,
            disableSsdOcrCardScan = false,
            automaticallyLaunchedCardScanFormDataHelper = helper,
            backupAction = backupAction,
        )
        val fakeEventsReporter = FakeCardScanEventsReporter()
        val scenario = Scenario(
            onScannedCardCalls = onScannedCardCalls,
            fakeEventsReporter = fakeEventsReporter,
        )

        mockStatic(PaymentCardRecognitionResult::class.java).use { mockedStatic ->
            mockedStatic.`when`<PaymentCardRecognitionResult> {
                PaymentCardRecognitionResult.getFromIntent(any())
            }.thenReturn(mockResult)
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides registryOwner,
                    LocalCardScanEventsReporter provides fakeEventsReporter,
                    LocalPaymentCardRecognitionClient provides paymentCardRecognitionClient
                ) {
                    action.Content(enabled = true, onScannedCard = onScannedCard)
                }
            }
        }

        scenario.block()
        onScannedCardCalls.ensureAllEventsConsumed()
    }

    private companion object {
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = "pk_test_123",
            stripeAccountId = "acct_123",
        )
    }
}
