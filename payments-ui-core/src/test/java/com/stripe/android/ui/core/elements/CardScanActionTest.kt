package com.stripe.android.ui.core.elements

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wallet.CreditCardExpirationDate
import com.google.android.gms.wallet.PaymentCardRecognitionResult
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.ui.core.cardscan.FakeCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.FakePaymentCardRecognitionClient
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cardscan.LocalPaymentCardRecognitionClient
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CardScanActionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

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
            verify(controller, times(1)).onScannedCard(
                cardNumber = "4242424242424242",
                expirationYear = 2042,
                expirationMonth = 2,
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
            verify(controller, never()).onScannedCard(any(), anyOrNull(), anyOrNull())
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
            verify(controller, never()).onScannedCard(any(), anyOrNull(), anyOrNull())
            assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
        }
    }

    @Test
    fun `does not launch card scan when automaticallyLaunchedCardScanFormDataHelper null`() {
        runScenario(helper = null) {
            verify(controller, never()).onScannedCard(any(), anyOrNull(), anyOrNull())
        }
    }

    private class Scenario(
        val controller: CardDetailsSectionController,
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
        val action = CardScanAction(
            isStripeCardScanAllowed = false,
            enableMlKitCardScan = false,
            disableSsdOcrCardScan = false,
            automaticallyLaunchedCardScanFormDataHelper = helper,
        )
        val controller = CardDetailsSectionController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = false,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            cardBrandFilter = DefaultCardBrandFilter,
            cardDetailsAction = action,
        )
        val controllerSpy = spy(controller)
        val scenario = Scenario(controller = controllerSpy)

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
                    action.Content(enabled = true, controllerSpy)
                }
            }
        }

        scenario.block()
    }
}
