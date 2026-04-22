package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
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
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.ui.core.R
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultFormActivityStateHelperTest {
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
    fun `state updates isEnabled when selection is set`() = testScenario {
        stateHolder.state.test {
            assertThat(awaitItem().isEnabled).isFalse()
            selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem().isEnabled).isTrue()
        }
    }

    @Test
    fun `state updates processing correctly while confirming`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionHolder.set(selection)

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
            selectionHolder.set(selection)

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
    fun `updateMandate updates mandateText`() = testScenario {
        stateHolder.state.test {
            awaitAndVerifyInitialState()

            stateHolder.updateMandate("Some new mandate".resolvableString)
            assertThat(awaitItem().mandateText).isEqualTo("Some new mandate".resolvableString)
        }
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

            selectionHolder.set(null)

            expectNoEvents()
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
                    FormResult.Complete(
                        selection = null,
                        hasBeenConfirmed = true,
                        customerState = customerStateHolder.customer.value,
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
                    FormResult.Complete(
                        selection = expectedSelection,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                    )
                )
            }
            assertThat(customerStateHolder.addPaymentMethodTurbine.awaitItem()).isEqualTo(
                expectedSelection.paymentMethod
            )
        }
    }

    @Test
    fun `TapToAddResult confirm saved payment method sets saved payment method confirm selection`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            stateHolder.state.test {
                awaitAndVerifyInitialState()

                tapToAddHelper.emitNextStep(
                    TapToAddNextStep.ConfirmSavedPaymentMethod(
                        paymentSelection = expectedSelection,
                    )
                )

                assertThat(awaitItem().savedPaymentSelectionToConfirm).isEqualTo(
                    expectedSelection,
                )
            }
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val stateHolder: DefaultFormActivityStateHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val onClickOverrideDelegate: OnClickOverrideDelegate,
    )

    private fun testScenario(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        config: EmbeddedPaymentElement.Configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
        tapToAddHelper: FakeTapToAddHelper = FakeTapToAddHelper.noOp(),
        customerStateHolder: FakeCustomerStateHolder = FakeCustomerStateHolder(),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(stripeIntent = stripeIntent)
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val onClickOverrideDelegate = OnClickDelegateOverrideImpl()
        val confirmationHandler = FakeConfirmationHandler()
        val stateHolder = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = config,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = onClickOverrideDelegate,
            eventReporter = FakeEventReporter(),
            confirmationHandler = confirmationHandler,
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        )

        Scenario(
            selectionHolder = selectionHolder,
            stateHolder = stateHolder,
            confirmationHandler = confirmationHandler,
            onClickOverrideDelegate = onClickOverrideDelegate,
        ).block()
    }

    private suspend fun TurbineTestContext<FormActivityStateHelper.State>.awaitAndVerifyInitialState() {
        val initialState = awaitItem()
        assertThat(initialState.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
        assertThat(initialState.isEnabled).isFalse()
        assertThat(initialState.isProcessing).isFalse()
    }
}
