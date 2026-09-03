package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddNextStep
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentelement.embedded.form.confirmationStateComplete
import com.stripe.android.paymentelement.embedded.form.confirmationStateConfirming
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

@Suppress("LargeClass")
internal class DefaultSheetActivityStateHolderTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val closeFormInteractorRule = CleanupTestRule(VerticalModeFormInteractor::close)

    @Test
    fun `state initializes correctly`() = testScenario {
        stateHolder.state.test {
            val state = awaitItem()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
            assertThat(state.isEnabled).isFalse()
            assertThat(state.primaryButtonLabel).isEqualTo(
                resolvableString(
                    id = R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("$10.99")
                )
            )
            assertThat(state.shouldDisplayLockIcon).isTrue()
        }
    }

    @Test
    fun `state is initialized correctly when formSheetAction=continue`() = testScenario(
        config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            .build()
    ) {
        stateHolder.state.test {
            val state = awaitItem()
            assertThat(state.primaryButtonLabel).isEqualTo(
                resolvableString(R.string.stripe_continue_button_label)
            )
            assertThat(state.shouldDisplayLockIcon).isFalse()
        }
    }

    @Test
    fun `state returns label from config if provided`() {
        testScenario(
            config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .primaryButtonLabel("Test Label")
                .build()
        ) {
            stateHolder.state.test {
                assertThat(awaitItem().primaryButtonLabel).isEqualTo("Test Label".resolvableString)
            }
        }
    }

    @Test
    fun `state returns label from config if provided when formSheetAction=continue`() {
        testScenario(
            config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
                .primaryButtonLabel("Test Label")
                .build()
        ) {
            stateHolder.state.test {
                assertThat(awaitItem().primaryButtonLabel).isEqualTo("Test Label".resolvableString)
            }
        }
    }

    @Test
    fun `state returns correct label for setup intent`() {
        testScenario(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD) {
            stateHolder.state.test {
                assertThat(awaitItem().primaryButtonLabel)
                    .isEqualTo(R.string.stripe_setup_button_label.resolvableString)
            }
        }
    }

    @Test
    fun `PaymentOptions mode always uses continue label regardless of formSheetAction`() {
        testScenario(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        ) {
            stateHolder.state.test {
                val state = awaitItem()
                assertThat(state.primaryButtonLabel).isEqualTo(
                    resolvableString(R.string.stripe_continue_button_label)
                )
                assertThat(state.shouldDisplayLockIcon).isFalse()
            }
        }
    }

    @Test
    fun `state updates isEnabled when selection is set`() = testScenario {
        stateHolder.state.test {
            assertThat(awaitItem().isEnabled).isFalse()
            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem().isEnabled).isTrue()
        }
    }

    @Test
    fun `state updates processing correctly while confirming`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionHolder.setSelection(selection)

            val enabledState = awaitItem()
            assertThat(enabledState.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
            assertThat(enabledState.isProcessing).isFalse()
            assertThat(enabledState.isEnabled).isTrue()

            confirmationHandler.state.value = confirmationStateConfirming(selection)
            val processingState = awaitItem()
            assertThat(processingState.isEnabled).isFalse()
            assertThat(processingState.processingState).isEqualTo(PrimaryButtonProcessingState.Processing)
            assertThat(processingState.isProcessing).isTrue()
        }
    }

    @Test
    fun `state updates when confirmation is successful`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()
            confirmationHandler.state.value = confirmationStateComplete(true)

            val completedState = awaitItem()
            assertThat(completedState.processingState).isEqualTo(PrimaryButtonProcessingState.Completed)
            assertThat(completedState.isProcessing).isFalse()
        }
    }

    @Test
    fun `state re-enables if confirmation fails`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()
            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionHolder.setSelection(selection)

            // State emitted from setting selection
            assertThat(awaitItem().isEnabled).isTrue()

            confirmationHandler.state.value = confirmationStateConfirming(selection)
            val processingState = awaitItem()
            assertThat(processingState.isProcessing).isTrue()
            assertThat(processingState.isEnabled).isFalse()
            assertThat(processingState.processingState).isEqualTo(PrimaryButtonProcessingState.Processing)

            confirmationHandler.state.value = confirmationStateComplete(false)
            val failedState = awaitItem()
            assertThat(failedState.isEnabled).isTrue()
            assertThat(failedState.isProcessing).isFalse()
            assertThat(failedState.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
            assertThat(failedState.error).isEqualTo("Something went wrong".resolvableString)
        }
    }

    @Test
    fun `confirming state clears errors`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            confirmationHandler.state.value = confirmationStateComplete(false)
            val failedState = awaitItem()
            assertThat(failedState.error).isEqualTo("Something went wrong".resolvableString)

            confirmationHandler.state.value =
                confirmationStateConfirming(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            val confirmingState = awaitItem()
            assertThat(confirmingState.error).isNull()
        }
    }

    @Test
    fun `canceled result clears errors`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            confirmationHandler.state.value = confirmationStateComplete(false)
            assertThat(awaitItem().error).isEqualTo("Something went wrong".resolvableString)

            confirmationHandler.state.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None
                )
            )
            val canceledState = awaitItem()
            assertThat(canceledState.error).isNull()
            assertThat(canceledState.isProcessing).isFalse()
        }
    }

    @Test
    fun `updateError updates error`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            stateHolder.updateError("Something went wrong".resolvableString)
            assertThat(awaitItem().error).isEqualTo("Something went wrong".resolvableString)
        }
    }

    @Test
    fun `updateProcessing starts processing and clears error`() = testScenario {
        stateHolder.updateError("Something went wrong".resolvableString)

        stateHolder.state.test {
            assertThat(awaitItem().error).isEqualTo("Something went wrong".resolvableString)

            stateHolder.updateProcessing(true)

            val state = awaitItem()
            assertThat(state.isProcessing).isTrue()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Processing)
            assertThat(state.isEnabled).isFalse()
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `updateProcessing stops processing and re-enables for selection`() = testScenario {
        val error = "Something went wrong".resolvableString
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        stateHolder.updateProcessing(true)
        stateHolder.updateError(error)

        stateHolder.state.test {
            assertThat(awaitItem().isProcessing).isTrue()

            stateHolder.updateProcessing(false)

            val state = awaitItem()
            assertThat(state.isProcessing).isFalse()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
            assertThat(state.isEnabled).isTrue()
            assertThat(state.error).isEqualTo(error)
        }
    }

    @Test
    fun `updateProcessing stops processing and stays disabled without selection`() = testScenario {
        stateHolder.updateProcessing(true)

        stateHolder.state.test {
            assertThat(awaitItem().isProcessing).isTrue()

            stateHolder.updateProcessing(false)

            assertThat(awaitItem().isEnabled).isFalse()
        }
    }

    @Test
    fun `updateMandate updates mandateText`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            stateHolder.updateMandate("Some new mandate".resolvableString)
            assertThat(awaitItem().mandateText).isEqualTo("Some new mandate".resolvableString)
        }
    }

    @Test
    fun `disabled primary button click requests form validation`() = testScenario {
        stateHolder.validationRequested.test {
            stateHolder.onPrimaryButtonDisabledClick()

            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `disabled primary button click invokes US bank validation`() = testScenario {
        var validationRequested = false
        stateHolder.updatePrimaryButton {
            PrimaryButton.UIState(
                label = "Continue".resolvableString,
                canClickWhileDisabled = true,
                onClick = {},
                onDisabledClick = { validationRequested = true },
                enabled = false,
                lockVisible = false,
            )
        }

        stateHolder.onPrimaryButtonDisabledClick()

        assertThat(validationRequested).isTrue()
    }

    @Test
    fun `updatePrimaryButton updates primary button state`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            stateHolder.updatePrimaryButton {
                PrimaryButton.UIState(
                    label = "Do something".resolvableString,
                    onClick = {},
                    enabled = true,
                    lockVisible = true
                )
            }

            val updateState = awaitItem()
            assertThat(updateState.isEnabled).isTrue()
            assertThat(updateState.primaryButtonLabel).isEqualTo("Do something".resolvableString)
            assertThat(onClickOverrideDelegate.onClickOverride).isNotNull()

            stateHolder.updatePrimaryButton { null }

            val nullState = awaitItem()
            assertThat(nullState.isEnabled).isFalse()
            assertThat(nullState.primaryButtonLabel).isEqualTo(
                resolvableString(
                    id = R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("$10.99")
                )
            )
            assertThat(onClickOverrideDelegate.onClickOverride).isNull()
        }
    }

    @Test
    fun `selection update to null does not emit event if primaryButtonUiState is not null`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            stateHolder.updatePrimaryButton {
                PrimaryButton.UIState(
                    label = "Do something".resolvableString,
                    onClick = {},
                    enabled = true,
                    lockVisible = true
                )
            }

            val updateState = awaitItem()
            assertThat(updateState.isEnabled).isTrue()

            selectionHolder.setSelection(null)

            expectNoEvents()
        }
    }

    @Test
    fun `setting selection to null disables button`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem().isEnabled).isTrue()

            selectionHolder.setSelection(null)
            assertThat(awaitItem().isEnabled).isFalse()
        }
    }

    @Test
    fun `setting selection while processing keeps button disabled`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            confirmationHandler.state.value =
                confirmationStateConfirming(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            val processingState = awaitItem()
            assertThat(processingState.isProcessing).isTrue()
            assertThat(processingState.isEnabled).isFalse()

            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            expectNoEvents()
        }
    }

    @Test
    fun `TapToAddResult ShowSavedPaymentMethods sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            stateHolder.result.test {
                tapToAddHelper.emitNextStep(
                    TapToAddNextStep.ShowSavedPaymentMethods(
                        paymentSelection = expectedSelection,
                    )
                )

                assertThat(awaitItem()).isEqualTo(
                    EmbeddedActivityResult.Complete(
                        temporarySelection = null,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        selection = expectedSelection,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                        checkoutSessionResponse = null,
                        shouldInvokeSelectionCallback = false,
                        launchMode = EmbeddedLaunchMode.Form(
                            selectedPaymentMethodCode = "card",
                        ),
                    )
                )
            }
        }
    }

    @Test
    fun `TapToAddResult Complete sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            stateHolder.result.test {
                tapToAddHelper.emitNextStep(TapToAddNextStep.Complete)

                assertThat(awaitItem()).isEqualTo(
                    EmbeddedActivityResult.Complete(
                        temporarySelection = null,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        selection = null,
                        hasBeenConfirmed = true,
                        customerState = customerStateHolder.customer.value,
                        checkoutSessionResponse = null,
                        shouldInvokeSelectionCallback = false,
                        launchMode = EmbeddedLaunchMode.Form(
                            selectedPaymentMethodCode = "card",
                        ),
                    )
                )
            }
        }
    }

    @Test
    fun `TapToAddResult Continue sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)

            stateHolder.result.test {
                tapToAddHelper.emitNextStep(
                    TapToAddNextStep.Continue(
                        paymentSelection = expectedSelection,
                    )
                )

                assertThat(awaitItem()).isEqualTo(
                    EmbeddedActivityResult.Complete(
                        temporarySelection = null,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        selection = expectedSelection,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                        checkoutSessionResponse = null,
                        shouldInvokeSelectionCallback = false,
                        launchMode = EmbeddedLaunchMode.Form(
                            selectedPaymentMethodCode = "card",
                        ),
                    )
                )
            }
            assertThat(customerStateHolder.addPaymentMethodTurbine.awaitItem()).isEqualTo(
                expectedSelection.paymentMethod
            )
        }
    }

    @Test
    fun `TapToAddResult confirm saved payment method replaces current screen`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val interactorFactory = RecordingSavedPaymentMethodConfirmInteractorFactory()
        val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        testScenario(
            tapToAddHelper = tapToAddHelper,
            initialScreen = createHorizontalPaymentOptionsScreen(),
            savedPaymentMethodConfirmInteractorFactory = interactorFactory,
        ) {
            stateHolder.result.test {
                navigator.screen.test {
                    assertThat(awaitItem()).isInstanceOf(
                        EmbeddedNavigator.Screen.HorizontalPaymentOptions::class.java
                    )

                    tapToAddHelper.emitNextStep(
                        TapToAddNextStep.ConfirmSavedPaymentMethod(
                            paymentSelection = expectedSelection,
                        )
                    )

                    assertThat(awaitItem()).isInstanceOf(
                        EmbeddedNavigator.Screen.SavedPaymentMethodConfirm::class.java
                    )
                    assertThat(navigator.canGoBack).isFalse()
                }

                expectNoEvents()
            }

            assertThat(interactorFactory.createCalls.awaitItem()).isEqualTo(expectedSelection)
            interactorFactory.createCalls.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `TapToAddResult confirm saved payment method preserves previous screen and closes replaced form`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val confirmInteractor = FakeSavedPaymentMethodConfirmInteractor()
        val interactorFactory = RecordingSavedPaymentMethodConfirmInteractorFactory(confirmInteractor)
        val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val paymentOptionsInteractor = FakePaymentMethodVerticalLayoutInteractor.create()
        val paymentOptionsScreen = createVerticalPaymentOptionsScreen(paymentOptionsInteractor)
        val formInteractor = RecordingVerticalModeFormInteractor()
        closeFormInteractorRule.track(formInteractor)
        val formScreen = createFormScreen(formInteractor)
        testScenario(
            tapToAddHelper = tapToAddHelper,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            initialBackStack = listOf(paymentOptionsScreen, formScreen),
            savedPaymentMethodConfirmInteractorFactory = interactorFactory,
        ) {
            stateHolder.result.test {
                navigator.screen.test {
                    assertThat(awaitItem()).isSameInstanceAs(formScreen)

                    tapToAddHelper.emitNextStep(
                        TapToAddNextStep.ConfirmSavedPaymentMethod(
                            paymentSelection = expectedSelection,
                        )
                    )

                    assertThat(awaitItem()).isInstanceOf(
                        EmbeddedNavigator.Screen.SavedPaymentMethodConfirm::class.java
                    )
                    assertThat(navigator.canGoBack).isTrue()
                    assertThat(formInteractor.closeCalls.awaitItem()).isEqualTo(Unit)

                    navigator.performAction(EmbeddedNavigator.Action.Back)

                    assertThat(awaitItem()).isSameInstanceAs(paymentOptionsScreen)
                    assertThat(navigator.canGoBack).isFalse()
                    assertThat(confirmInteractor.closeCalls.awaitItem()).isEqualTo(Unit)
                }

                expectNoEvents()
            }

            assertThat(interactorFactory.createCalls.awaitItem()).isEqualTo(expectedSelection)
            interactorFactory.validate()
            formInteractor.validate()
            confirmInteractor.validate()
            paymentOptionsInteractor.validate()
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val stateHolder: DefaultSheetActivityStateHolder,
        val confirmationHandler: FakeConfirmationHandler,
        val onClickOverrideDelegate: OnClickOverrideDelegate,
        val navigator: EmbeddedNavigator,
    )

    private fun testScenario(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        config: EmbeddedPaymentElement.Configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
        tapToAddHelper: FakeTapToAddHelper = FakeTapToAddHelper.noOp(),
        customerStateHolder: FakeCustomerStateHolder = FakeCustomerStateHolder(),
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.Form(
            selectedPaymentMethodCode = "card",
        ),
        initialScreen: EmbeddedNavigator.Screen = EmbeddedNavigator.Screen.ManageAll(
            FakeManageScreenInteractor()
        ),
        initialBackStack: List<EmbeddedNavigator.Screen> = listOf(initialScreen),
        savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory =
            FakeSavedPaymentMethodConfirmInteractor.Factory(),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(stripeIntent = stripeIntent)
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val onClickOverrideDelegate = OnClickDelegateOverrideImpl()
        val confirmationHandler = FakeConfirmationHandler()
        val viewModelScope = TestScope(UnconfinedTestDispatcher())
        val navigator = EmbeddedNavigator(
            coroutineScope = viewModelScope,
            initialBackStack = initialBackStack,
            eventReporter = FakeEventReporter(),
        )
        lateinit var screenFactory: SavedPaymentMethodConfirmScreenFactory
        val stateHolder = DefaultSheetActivityStateHolder(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = config,
            coroutineScope = viewModelScope,
            onClickDelegate = onClickOverrideDelegate,
            eventReporter = FakeEventReporter(),
            confirmationHandler = confirmationHandler,
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
            embeddedNavigatorProvider = Provider { navigator },
            savedPaymentMethodConfirmScreenFactoryProvider = Provider { screenFactory },
        )
        screenFactory = SavedPaymentMethodConfirmScreenFactory(
            interactorFactory = savedPaymentMethodConfirmInteractorFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            sheetActivityStateHolder = stateHolder,
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
        )

        Scenario(
            selectionHolder = selectionHolder,
            stateHolder = stateHolder,
            confirmationHandler = confirmationHandler,
            onClickOverrideDelegate = onClickOverrideDelegate,
            navigator = navigator,
        ).block()
    }

    private class RecordingSavedPaymentMethodConfirmInteractorFactory(
        private val interactor: SavedPaymentMethodConfirmInteractor = FakeSavedPaymentMethodConfirmInteractor(),
    ) : SavedPaymentMethodConfirmInteractor.Factory {
        val createCalls = Turbine<PaymentSelection.Saved>()

        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit,
        ): SavedPaymentMethodConfirmInteractor {
            createCalls.add(initialSelection)
            return interactor
        }

        fun validate() {
            createCalls.ensureAllEventsConsumed()
        }
    }

    private class RecordingVerticalModeFormInteractor : VerticalModeFormInteractor {
        override val isLiveMode: Boolean = true
        override val state: StateFlow<VerticalModeFormInteractor.State>
            get() = error("Not expected")

        val handleViewActionCalls = Turbine<VerticalModeFormInteractor.ViewAction>()
        val closeCalls = Turbine<Unit>()

        override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
            handleViewActionCalls.add(viewAction)
        }

        override fun close() {
            closeCalls.add(Unit)
        }

        fun validate() {
            handleViewActionCalls.ensureAllEventsConsumed()
            closeCalls.ensureAllEventsConsumed()
        }
    }

    private fun createFormScreen(
        interactor: VerticalModeFormInteractor,
    ): EmbeddedNavigator.Screen.Form {
        return EmbeddedNavigator.Screen.Form(
            formInteractor = interactor,
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
    }

    private fun createVerticalPaymentOptionsScreen(
        interactor: FakePaymentMethodVerticalLayoutInteractor,
    ): EmbeddedNavigator.Screen.VerticalPaymentOptions {
        return EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = interactor,
            isLiveMode = true,
            sheetActivityState = stateFlowOf(
                SheetActivityStateHolder.State(
                    primaryButtonLabel = "Continue".resolvableString,
                    isEnabled = true,
                    processingState = PrimaryButtonProcessingState.Idle(null),
                    isProcessing = false,
                    shouldDisplayLockIcon = false,
                )
            ),
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private fun createHorizontalPaymentOptionsScreen(): EmbeddedNavigator.Screen.HorizontalPaymentOptions {
        return EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = FakeAddPaymentMethodInteractor(FakeAddPaymentMethodInteractor.createState()),
            sheetActivityState = stateFlowOf(
                SheetActivityStateHolder.State(
                    primaryButtonLabel = "Continue".resolvableString,
                    isEnabled = true,
                    processingState = PrimaryButtonProcessingState.Idle(null),
                    isProcessing = false,
                    shouldDisplayLockIcon = false,
                )
            ),
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private suspend fun TurbineTestContext<SheetActivityStateHolder.State>.awaitAndVerifyInitialState() {
        val initialState = awaitItem()
        assertThat(initialState.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
        assertThat(initialState.isEnabled).isFalse()
        assertThat(initialState.isProcessing).isFalse()
    }
}
