package com.stripe.android.paymentelement.embedded.sheet

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class SavedPaymentMethodConfirmScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `primary button click confirms payment`() = runTest {
        runScenario {
            composeRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).performClick()

            assertThat(confirmationHelper.confirmCalls.awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `confirmation error is displayed`() = runTest {
        runScenario(
            initialState = defaultState().copy(
                error = "Something went wrong".resolvableString,
            ),
        ) {
            composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        }
    }

    @Test
    fun `processing dismisses keyboard`() = runTest {
        val keyboardController = FakeSoftwareKeyboardController()
        runScenario(
            initialState = defaultState().copy(
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Processing,
                isProcessing = true,
            ),
            keyboardController = keyboardController,
        ) {
            composeRule.waitForIdle()

            assertThat(keyboardController.hideCalls.awaitItem()).isEqualTo(Unit)
            keyboardController.validate()
        }
    }

    @Test
    fun `completed processing emits successful activity result`() = runTest {
        runScenario(
            initialState = defaultState().copy(
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Completed,
                isProcessing = true,
            ),
        ) {
            composeRule.mainClock.advanceTimeBy(5_000)
            composeRule.waitForIdle()

            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result.selection).isNull()
            assertThat(result.previousNewSelections).isEqualTo(selectionHolder.previousNewSelections)
            assertThat(result.hasBeenConfirmed).isTrue()
            assertThat(result.customerState).isEqualTo(customerStateHolder.customer.value)
            assertThat(result.shouldInvokeSelectionCallback).isFalse()
            assertThat(result.launchMode).isEqualTo(launchMode)
        }
    }

    @Test
    fun `completed processing preserves PaymentOptions launch mode`() = runTest {
        val launchMode = EmbeddedLaunchMode.PaymentOptions
        runScenario(
            initialState = defaultState().copy(
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Completed,
                isProcessing = true,
            ),
            launchMode = launchMode,
        ) {
            composeRule.mainClock.advanceTimeBy(5_000)
            composeRule.waitForIdle()

            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result.launchMode).isEqualTo(launchMode)
        }
    }

    private suspend fun runScenario(
        initialState: SheetActivityStateHolder.State = defaultState(),
        keyboardController: SoftwareKeyboardController? = null,
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.Form(
            selectedPaymentMethodCode = "card",
        ),
        block: suspend Scenario.() -> Unit,
    ) {
        val interactor = FakeSavedPaymentMethodConfirmInteractor(formEnabled = true)
        val stateHolder = FakeSheetActivityStateHolder(initialState)
        val confirmationHelper = FakeSheetActivityConfirmationHelper()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val customerStateHolder = FakeCustomerStateHolder()
        val eventReporter = FakeEventReporter()
        val screen = EmbeddedNavigator.Screen.SavedPaymentMethodConfirm(
            interactor = interactor,
            isLiveMode = true,
            eventReporter = eventReporter,
            sheetActivityStateHolder = stateHolder,
            confirmationHelper = confirmationHelper,
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides keyboardController,
            ) {
                StripeTheme {
                    screen.Content()
                }
            }
        }

        Scenario(
            stateHolder = stateHolder,
            confirmationHelper = confirmationHelper,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
        ).block()

        stateHolder.validate()
        confirmationHelper.validate()
        interactor.validate()
        eventReporter.validate()
    }

    private fun defaultState() = SheetActivityStateHolder.State(
        primaryButtonLabel = "Pay".resolvableString,
        isEnabled = true,
        processingState = PrimaryButtonProcessingState.Idle(null),
        isProcessing = false,
        shouldDisplayLockIcon = true,
    )

    private class FakeSoftwareKeyboardController : SoftwareKeyboardController {
        val hideCalls = Turbine<Unit>()

        override fun show() = Unit

        override fun hide() {
            hideCalls.add(Unit)
        }

        fun validate() {
            hideCalls.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val stateHolder: FakeSheetActivityStateHolder,
        val confirmationHelper: FakeSheetActivityConfirmationHelper,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
        val launchMode: EmbeddedLaunchMode,
    )
}
