package com.stripe.android.paymentelement.embedded.sheet

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentOptionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `vertical options update Continue visibility and display mandate`() = runScenario(
        screenFactory = { state, onContinue ->
            EmbeddedNavigator.Screen.VerticalPaymentOptions(
                interactor = FakePaymentMethodVerticalLayoutInteractor.create(),
                sheetActivityState = state,
                onContinueClick = onContinue,
                onPrimaryButtonDisabledClick = {},
            )
        },
    ) {
        assertConditionalContinueAndMandate()
    }

    @Test
    fun `horizontal form keeps Continue visible and displays mandate`() = runScenario(
        screenFactory = { state, onContinue ->
            EmbeddedNavigator.Screen.HorizontalPaymentOptions(
                interactor = FakeAddPaymentMethodInteractor(FakeAddPaymentMethodInteractor.createState()),
                sheetActivityState = state,
                onContinueClick = onContinue,
                onPrimaryButtonDisabledClick = {},
            )
        },
    ) {
        composeRule.runOnIdle {
            state.value = state.value.copy(
                isSelectionButtonVisible = false,
                mandateText = "Mandate".resolvableString,
            )
        }
        composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).performScrollTo().performClick()
        assertThat(continueCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `horizontal saved options update Continue visibility and display mandate`() = runScenario(
        screenFactory = { state, onContinue ->
            savedScreen(state, onContinue, PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        },
    ) {
        assertConditionalContinueAndMandate()
    }

    @Test
    fun `saved SEPA mandate appears above Continue`() = runScenario(
        initialState = defaultState().copy(mandateText = "SEPA mandate".resolvableString),
        screenFactory = { state, onContinue ->
            savedScreen(state, onContinue, PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        },
    ) {
        val mandate = composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).fetchSemanticsNode().boundsInRoot
        assertThat(mandate.bottom).isAtMost(button.top)
    }

    @Test
    fun `saved US bank mandate appears below Continue`() = runScenario(
        initialState = defaultState().copy(mandateText = "Bank mandate".resolvableString),
        screenFactory = { state, onContinue -> savedScreen(state, onContinue, PaymentMethodFixtures.US_BANK_ACCOUNT) },
    ) {
        val mandate = composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).fetchSemanticsNode().boundsInRoot
        assertThat(mandate.top).isAtLeast(button.bottom)
    }

    private suspend fun Scenario.assertConditionalContinueAndMandate() {
        composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { state.value = state.value.copy(isSelectionButtonVisible = false) }
        composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).assertDoesNotExist()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                isSelectionButtonVisible = true,
                mandateText = "Mandate".resolvableString,
            )
        }
        composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG).performScrollTo().performClick()
        assertThat(continueCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun savedScreen(
        state: MutableStateFlow<SheetActivityStateHolder.State>,
        onContinue: () -> Unit,
        paymentMethod: PaymentMethod,
    ): EmbeddedNavigator.Screen {
        val item = PaymentOptionsItem.SavedPaymentMethod(paymentMethod.toDisplayableSavedPaymentMethod())
        return EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
            interactor = FakeSelectSavedPaymentMethodsInteractor(
                initialState = SelectSavedPaymentMethodsInteractor.State(
                    paymentOptionsItems = listOf(item),
                    selectedPaymentOptionsItem = item,
                    linkBrand = LinkBrand.Link,
                    isEditing = false,
                    isProcessing = false,
                    canEdit = false,
                    canRemove = false,
                ),
            ),
            sheetActivityState = state,
            onContinueClick = onContinue,
            onPrimaryButtonDisabledClick = {},
        )
    }

    private fun runScenario(
        initialState: SheetActivityStateHolder.State = defaultState(),
        screenFactory: (MutableStateFlow<SheetActivityStateHolder.State>, () -> Unit) -> EmbeddedNavigator.Screen,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val state = MutableStateFlow(initialState)
        val continueCalls = Turbine<Unit>()
        val eventReporter = FakeEventReporter()
        val screen = screenFactory(state) { continueCalls.add(Unit) }
        composeRule.setContent {
            StripeTheme {
                EventReporterProvider(eventReporter) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        screen.Content(showsWalletsHeader = false)
                    }
                }
            }
        }
        Scenario(state, continueCalls).block()
        continueCalls.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    private data class Scenario(
        val state: MutableStateFlow<SheetActivityStateHolder.State>,
        val continueCalls: Turbine<Unit>,
    )

    private fun defaultState() = SheetActivityStateHolder.State(
        primaryButtonLabel = "Continue".resolvableString,
        isEnabled = true,
        isSelectionButtonVisible = true,
        processingState = PrimaryButtonProcessingState.Idle(null),
        isProcessing = false,
        primaryButtonIcon = PrimaryButtonIcon.None,
    )
}
