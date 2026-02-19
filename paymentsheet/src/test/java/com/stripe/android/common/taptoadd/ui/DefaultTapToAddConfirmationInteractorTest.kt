package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkFormElement
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class DefaultTapToAddConfirmationInteractorTest {
    @Test
    fun `state has card brand and last4 from payment method`() = runScenario(
        paymentMethod = PaymentMethodFactory.card().update(
            last4 = "1234",
            brand = CardBrand.MasterCard,
            addCbcNetworks = false,
        ),
        tapToAddMode = TapToAddMode.Continue,
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.cardBrand).isEqualTo(CardBrand.MasterCard)
            assertThat(state.last4).isEqualTo("1234")
        }
    }

    @Test
    fun `expected initial state in Complete mode`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 5099,
            ),
            isTapToAddSupported = true,
        ),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.title).isEqualTo(
                resolvableString(
                    StripeUiCoreR.string.stripe_pay_button_amount,
                    "$50.99"
                )
            )
            assertThat(state.primaryButton.locked).isTrue()
            assertThat(state.primaryButton.label).isEqualTo(
                R.string.stripe_paymentsheet_pay_button_label.resolvableString
            )
            assertThat(state.primaryButton.locked).isTrue()
        }
    }

    @Test
    fun `expected initial state in Continue mode`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Continue,
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.title)
                .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
            assertThat(state.primaryButton.label)
                .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
            assertThat(state.primaryButton.locked).isFalse()
        }
    }

    @Test
    fun `PrimaryButtonPressed in continue mode calls onContinue with saved selection`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Continue,
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)

        val receivedSelection = onContinueCalls.awaitItem()

        assertThat(receivedSelection.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `PrimaryButtonPressed in Complete mode when idle starts confirmation process`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        tapToAddMode = TapToAddMode.Complete,
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)

        val args = confirmationHandlerScenario.startTurbine.awaitItem()

        assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        val confirmationOption = args.confirmationOption as PaymentMethodConfirmationOption.Saved

        assertThat(confirmationOption.paymentMethod).isEqualTo(paymentMethod)
        assertThat(args.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `PrimaryButtonPressed in Complete mode when not Idle does not call confirmationHandler start`() {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        runScenario(
            paymentMethod = paymentMethod,
            tapToAddMode = TapToAddMode.Complete,
            initialConfirmationState = ConfirmationHandler.State.Confirming(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
                ),
            ),
        ) {
            interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)
            confirmationHandlerScenario.startTurbine.expectNoEvents()
        }
    }

    @Test
    fun `ShownSuccess calls onComplete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Continue,
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.ShownSuccess)

        assertThat(onCompleteCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `state updates to Processing when confirmation is in progress`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle)

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Confirming(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
                ),
            )

            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing)
        }
    }

    @Test
    fun `state updates to Complete when confirmation succeeds`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle)

            val intent = PaymentIntentFixtures.PI_SUCCEEDED

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(intent),
            )

            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Complete)

            val paymentSuccessEventCall = eventReporter.paymentSuccessCalls.awaitItem()

            assertThat(paymentSuccessEventCall.paymentSelection)
                .isEqualTo(PaymentSelection.Saved(paymentMethod))
            assertThat(paymentSuccessEventCall.intentId).isEqualTo(intent.id)
        }
    }

    @Test
    fun `state updates to Idle with error when confirmations fails`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
    ) {
        val errorMessage = "Payment failed".resolvableString
        interactor.state.test {
            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle)

            val exception = Exception("Payment failed")

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = exception,
                    message = errorMessage,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                ),
            )

            val state = awaitItem()
            assertThat(state.primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle)
            assertThat(state.error).isEqualTo(errorMessage)

            val paymentFailureEventCall = eventReporter.paymentFailureCalls.awaitItem()

            assertThat(paymentFailureEventCall.paymentSelection)
                .isEqualTo(PaymentSelection.Saved(paymentMethod))
            assertThat(paymentFailureEventCall.error)
                .isEqualTo(PaymentSheetConfirmationError.Stripe(exception))
        }
    }

    @Test
    fun `primary button disabled when link form state is Incomplete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
        linkFormHelper = FakeSavedPaymentMethodLinkFormHelper(
            initialState = SavedPaymentMethodLinkFormHelper.State.Incomplete,
        ),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isFalse()
        }
    }

    @Test
    fun `primary button enabled when link form state is Unused`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
        linkFormHelper = FakeSavedPaymentMethodLinkFormHelper(
            initialState = SavedPaymentMethodLinkFormHelper.State.Unused,
        ),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `primary button enabled when link form state is Complete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
        linkFormHelper = FakeSavedPaymentMethodLinkFormHelper(
            initialState = SavedPaymentMethodLinkFormHelper.State.Complete(
                userInput = UserInput.SignIn(email = "email@email.com")
            ),
        ),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `form elements are empty when link form helper has no link form element`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode = TapToAddMode.Complete,
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.form.elements).isEmpty()
        }
    }

    private fun runScenario(
        paymentMethod: PaymentMethod,
        tapToAddMode: TapToAddMode = TapToAddMode.Complete,
        paymentMethodMetadata: PaymentMethodMetadata =
            PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        initialConfirmationState: ConfirmationHandler.State = ConfirmationHandler.State.Idle,
        linkFormHelper: SavedPaymentMethodLinkFormHelper = FakeSavedPaymentMethodLinkFormHelper(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        FakeConfirmationHandler.test(
            initialState = initialConfirmationState,
        ) {
            val onCompleteCalls = Turbine<Unit>()
            val onContinueCalls = Turbine<PaymentSelection.Saved>()

            val interactor = DefaultTapToAddConfirmationInteractor(
                coroutineScope = backgroundScope,
                tapToAddMode = tapToAddMode,
                paymentMethod = paymentMethod,
                paymentMethodMetadata = paymentMethodMetadata,
                confirmationHandler = handler,
                eventReporter = eventReporter,
                linkFormHelper = linkFormHelper,
                onContinue = {
                    onContinueCalls.add(it)
                },
                onComplete = {
                    onCompleteCalls.add(Unit)
                },
            )

            block(
                Scenario(
                    interactor = interactor,
                    paymentMethod = paymentMethod,
                    paymentMethodMetadata = paymentMethodMetadata,
                    confirmationHandlerScenario = this,
                    onCompleteCalls = onCompleteCalls,
                    onContinueCalls = onContinueCalls,
                    eventReporter = eventReporter,
                )
            )

            eventReporter.validate()
            onCompleteCalls.ensureAllEventsConsumed()
            onContinueCalls.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val interactor: TapToAddConfirmationInteractor,
        val paymentMethod: PaymentMethod,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val confirmationHandlerScenario: FakeConfirmationHandler.Scenario,
        val onCompleteCalls: ReceiveTurbine<Unit>,
        val onContinueCalls: ReceiveTurbine<PaymentSelection.Saved>,
        val eventReporter: FakeEventReporter,
    )

    private class FakeSavedPaymentMethodLinkFormHelper(
        initialState: SavedPaymentMethodLinkFormHelper.State = SavedPaymentMethodLinkFormHelper.State.Unused,
        override val formElement: LinkFormElement? = null,
    ) : SavedPaymentMethodLinkFormHelper {
        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<SavedPaymentMethodLinkFormHelper.State> = _state.asStateFlow()
    }
}
