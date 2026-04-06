package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.CvcFormHelper
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationOption
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.PaymentIntentFactory
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
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 5099,
            ),
            isTapToAddSupported = true,
        ),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.label).isEqualTo(
                resolvableString(
                    StripeUiCoreR.string.stripe_pay_button_amount,
                    "$50.99"
                )
            )
            assertThat(state.primaryButton.locked).isTrue()
        }
    }

    @Test
    fun `expected initial state has primary button locked`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.locked).isTrue()
        }
    }

    @Test
    fun `Complete mode with link input and link state in metadata confirms with link option`() =
        runScenario(
            paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
            linkInput = UserInput.SignIn(email = "link@test.com"),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                isTapToAddSupported = true,
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
            ),
        ) {
            interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)

            assertThat(eventReporter.tapToAddConfirmCalls.awaitItem()).isNotNull()
            val args = confirmationHandlerScenario.startTurbine.awaitItem()

            assertThat(args.confirmationOption).isInstanceOf<LinkInlineSignupConfirmationOption.Saved>()
            val savedOption = args.confirmationOption as LinkInlineSignupConfirmationOption.Saved
            assertThat(savedOption.paymentMethod).isEqualTo(paymentMethod)
            assertThat(savedOption.sanitizedUserInput)
                .isEqualTo(UserInput.SignIn(email = "link@test.com"))
        }

    @Test
    fun `PrimaryButtonPressed in Complete mode when idle starts confirmation process`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)

        assertThat(eventReporter.tapToAddConfirmCalls.awaitItem()).isNotNull()
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
    fun `state updates to Processing when confirmation is in progress`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
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
    fun `state calls complete when confirmation succeeds`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle)

            val intent = PaymentIntentFixtures.PI_SUCCEEDED

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(intent),
            )

            assertThat(awaitItem().primaryButton.state)
                .isEqualTo(TapToAddConfirmationInteractor.State.PrimaryButton.State.Success)

            val paymentSuccessEventCall = eventReporter.paymentSuccessCalls.awaitItem()

            assertThat(paymentSuccessEventCall.paymentSelection)
                .isEqualTo(
                    PaymentSelection.Saved(
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(cvc = "123"),
                    )
                )
            assertThat(paymentSuccessEventCall.intentId).isEqualTo(intent.id)
        }
    }

    @Test
    fun `state updates to Idle with error when confirmations fails`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
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
                .isEqualTo(
                    PaymentSelection.Saved(
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(cvc = "123"),
                    )
                )
            assertThat(paymentFailureEventCall.error)
                .isEqualTo(PaymentSheetConfirmationError.Stripe(exception))
        }
    }

    @Test
    fun `primary button disabled when cvc form state is Incomplete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        initialCvcState = CvcFormHelper.State.Incomplete,
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isFalse()
        }
    }

    @Test
    fun `primary button enabled when cvc form state is NotRequired`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        initialCvcState = CvcFormHelper.State.NotRequired,
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `primary button enabled when cvc form state is Complete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        initialCvcState = CvcFormHelper.State.Complete("123"),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `Complete mode includes provided payment method options in confirmation option`() =
        runScenario(
            paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
            initialCvcState = CvcFormHelper.State.Complete("123"),
        ) {
            interactor.performAction(TapToAddConfirmationInteractor.Action.PrimaryButtonPressed)

            assertThat(eventReporter.tapToAddConfirmCalls.awaitItem()).isNotNull()
            val args = confirmationHandlerScenario.startTurbine.awaitItem()

            assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

            val confirmationOption = args.confirmationOption as PaymentMethodConfirmationOption.Saved

            assertThat(confirmationOption.paymentMethod).isEqualTo(paymentMethod)
            assertThat(confirmationOption.optionsParams).isEqualTo(
                PaymentMethodOptionsParams.Card(cvc = "123")
            )
        }

    @Test
    fun `form elements are empty when cvc form helper has no form element`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.form.elements).isEmpty()
        }
    }

    @Test
    fun `form is enabled when confirmation state is Idle`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            val state = awaitItem()
            assertThat(state.form.enabled).isTrue()
        }
    }

    @Test
    fun `form is disabled when confirmation state is Confirming`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().form.enabled).isTrue()

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Confirming(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
                ),
            )

            assertThat(awaitItem().form.enabled).isFalse()
        }
    }

    @Test
    fun `form is disabled when confirmation succeeds`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().form.enabled).isTrue()

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED),
            )

            assertThat(awaitItem().form.enabled).isFalse()
            assertThat(eventReporter.paymentSuccessCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `form is enabled when confirmation fails so user can retry`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().form.enabled).isTrue()

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = Exception("Payment failed"),
                    message = "Payment failed".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                ),
            )

            assertThat(awaitItem().form.enabled).isTrue()
            assertThat(eventReporter.paymentFailureCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `onSuccessShown should call not onComplete if confirmation is not complete`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        initialConfirmationState = ConfirmationHandler.State.Idle
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.SuccessShown)
        onCompleteCalls.expectNoEvents()
    }

    @Test
    fun `onSuccessShown should call onComplete if confirmation is completed successfully`() = runScenario(
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        initialConfirmationState = ConfirmationHandler.State.Complete(
            result = ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFactory.create(
                    status = StripeIntent.Status.Succeeded,
                ),
            ),
        )
    ) {
        interactor.performAction(TapToAddConfirmationInteractor.Action.SuccessShown)
        assertThat(onCompleteCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `close should stop confirmation state and CVC form helper updates from being received`() = runScenario(
        initialCvcState = CvcFormHelper.State.Incomplete,
        paymentMethod = PaymentMethodFactory.card(last4 = "4242"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.enabled).isFalse()

            interactor.close()

            confirmationHandlerScenario.confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = Exception("Payment failed"),
                    message = "Payment failed".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                ),
            )
            cvcFormHelper.updateState(CvcFormHelper.State.Complete(cvc = "123"))

            expectNoEvents()
        }
    }

    private fun runScenario(
        paymentMethod: PaymentMethod,
        linkInput: UserInput? = null,
        paymentMethodMetadata: PaymentMethodMetadata =
            PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        initialConfirmationState: ConfirmationHandler.State = ConfirmationHandler.State.Idle,
        initialCvcState: CvcFormHelper.State = CvcFormHelper.State.Complete("123"),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        val cvcFormHelper = FakeCvcFormHelper(initialState = initialCvcState)
        FakeConfirmationHandler.test(
            initialState = initialConfirmationState,
        ) {
            val onCompleteCalls = Turbine<Unit>()

            val interactor = DefaultTapToAddConfirmationInteractor(
                coroutineContext = coroutineContext,
                paymentMethod = paymentMethod,
                linkInput = linkInput,
                paymentMethodMetadata = paymentMethodMetadata,
                confirmationHandler = handler,
                cvcFormHelper = cvcFormHelper,
                eventReporter = eventReporter,
                onComplete = {
                    onCompleteCalls.add(Unit)
                },
            )

            block(
                Scenario(
                    interactor = interactor,
                    paymentMethod = paymentMethod,
                    paymentMethodMetadata = paymentMethodMetadata,
                    cvcFormHelper = cvcFormHelper,
                    confirmationHandlerScenario = this,
                    onCompleteCalls = onCompleteCalls,
                    eventReporter = eventReporter,
                )
            )

            eventReporter.validate()
            onCompleteCalls.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val interactor: TapToAddConfirmationInteractor,
        val paymentMethod: PaymentMethod,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val confirmationHandlerScenario: FakeConfirmationHandler.Scenario,
        val cvcFormHelper: FakeCvcFormHelper,
        val onCompleteCalls: ReceiveTurbine<Unit>,
        val eventReporter: FakeEventReporter,
    )

    private class FakeCvcFormHelper(
        initialState: CvcFormHelper.State,
    ) : CvcFormHelper {
        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<CvcFormHelper.State> = _state.asStateFlow()
        override val formElement: com.stripe.android.uicore.elements.FormElement? = null

        fun updateState(state: CvcFormHelper.State) {
            _state.value = state
        }
    }
}
