package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class DefaultTapToAddCardAddedInteractorTest {
    @Test
    fun `initial state is correct`() = runScenario(
        paymentMethod = PaymentMethodFactory.card().update(
            last4 = "1234",
            brand = CardBrand.MasterCard,
            addCbcNetworks = false,
        ),
        initialLinkState = SavedPaymentMethodLinkFormHelper.State.Unused,
    ) {
        val state = interactor.state.value
        assertThat(state.cardBrand).isEqualTo(CardBrand.MasterCard)
        assertThat(state.last4).isEqualTo("1234")
        assertThat(state.title).isEqualTo(R.string.stripe_tap_to_add_card_added_title.resolvableString)
        assertThat(state.primaryButton.label)
            .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
        assertThat(state.primaryButton.enabled).isTrue()
        assertThat(state.form.elements).containsExactly(fakeLinkFormHelper.formElement)
        assertThat(state.form.enabled).isTrue()
    }

    @Test
    fun `state primary button is disabled when link helper is Incomplete`() = runScenario(
        initialLinkState = SavedPaymentMethodLinkFormHelper.State.Complete(mockUserInput()),
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.enabled).isTrue()
            fakeLinkFormHelper.updateState(SavedPaymentMethodLinkFormHelper.State.Incomplete)
            assertThat(awaitItem().primaryButton.enabled).isFalse()
        }
    }

    @Test
    fun `state primary button is enabled when link helper emits Unused`() = runScenario(
        initialLinkState = SavedPaymentMethodLinkFormHelper.State.Incomplete,
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.enabled).isFalse()
            fakeLinkFormHelper.updateState(SavedPaymentMethodLinkFormHelper.State.Unused)
            assertThat(awaitItem().primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `PrimaryButtonPressed in Continue mode calls onContinue with payment method`() =
        runScenario(
            tapToAddMode = TapToAddMode.Continue,
            initialLinkState = SavedPaymentMethodLinkFormHelper.State.Unused,
        ) {
            interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)

            val selection = onContinueCalls.awaitItem()
            assertThat(selection.paymentMethod).isEqualTo(paymentMethod)
            assertThat(selection.linkInput).isNull()
        }

    @Test
    fun `PrimaryButtonPressed in Continue mode calls onContinue with payment method and link input`() =
        runScenario(
            tapToAddMode = TapToAddMode.Continue,
            initialLinkState = SavedPaymentMethodLinkFormHelper.State.Complete(mockUserInput()),
        ) {
            interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)

            val selection = onContinueCalls.awaitItem()
            assertThat(selection.paymentMethod).isEqualTo(paymentMethod)
            assertThat(selection.linkInput).isEqualTo(mockUserInput())
        }

    @Test
    fun `PrimaryButtonPressed in Complete mode calls onConfirm with payment method`() =
        runScenario(
            tapToAddMode = TapToAddMode.Complete,
            initialLinkState = SavedPaymentMethodLinkFormHelper.State.Unused,
        ) {
            interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)

            val (paymentMethod, linkInput) = onConfirmCalls.awaitItem()
            assertThat(paymentMethod).isEqualTo(paymentMethod)
            assertThat(linkInput).isNull()
        }

    @Test
    fun `PrimaryButtonPressed in Complete mode calls onConfirm with payment method and link input`() =
        runScenario(
            tapToAddMode = TapToAddMode.Complete,
            initialLinkState = SavedPaymentMethodLinkFormHelper.State.Complete(mockUserInput()),
        ) {
            interactor.performAction(TapToAddCardAddedInteractor.Action.PrimaryButtonPressed)

            val (confirmedPaymentMethod, confirmedLinkInput) = onConfirmCalls.awaitItem()
            assertThat(confirmedPaymentMethod).isEqualTo(paymentMethod)
            assertThat(confirmedLinkInput).isEqualTo(mockUserInput())
        }

    private fun mockUserInput(): UserInput = UserInput.SignIn(email = "test@test.com")

    private fun runScenario(
        paymentMethod: PaymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode: TapToAddMode = TapToAddMode.Continue,
        initialLinkState: SavedPaymentMethodLinkFormHelper.State = SavedPaymentMethodLinkFormHelper.State.Unused,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onContinueCalls = Turbine<PaymentSelection.Saved>()
        val onConfirmCalls = Turbine<Pair<PaymentMethod, UserInput?>>()
        val fakeLinkFormHelper = FakeSavedPaymentMethodLinkFormHelper(
            initialState = initialLinkState,
            formElement = FakeFormElement,
        )

        val scenario = Scenario(
            paymentMethod = paymentMethod,
            interactor = DefaultTapToAddCardAddedInteractor(
                coroutineScope = backgroundScope,
                tapToAddMode = tapToAddMode,
                paymentMethod = paymentMethod,
                savedPaymentMethodLinkFormHelper = fakeLinkFormHelper,
                onContinue = { onContinueCalls.add(it) },
                onConfirm = { pm, linkInput -> onConfirmCalls.add(pm to linkInput) },
            ),
            fakeLinkFormHelper = fakeLinkFormHelper,
            onContinueCalls = onContinueCalls,
            onConfirmCalls = onConfirmCalls,
        )
        block(scenario)

        onContinueCalls.ensureAllEventsConsumed()
        onConfirmCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val paymentMethod: PaymentMethod,
        val interactor: TapToAddCardAddedInteractor,
        val fakeLinkFormHelper: FakeSavedPaymentMethodLinkFormHelper,
        val onContinueCalls: ReceiveTurbine<PaymentSelection.Saved>,
        val onConfirmCalls: ReceiveTurbine<Pair<PaymentMethod, UserInput?>>,
    )

    private class FakeSavedPaymentMethodLinkFormHelper(
        initialState: SavedPaymentMethodLinkFormHelper.State,
        override val formElement: FormElement,
    ) : SavedPaymentMethodLinkFormHelper {
        private val _state = MutableStateFlow(initialState)

        override val state: StateFlow<SavedPaymentMethodLinkFormHelper.State> = _state

        fun updateState(state: SavedPaymentMethodLinkFormHelper.State) {
            _state.value = state
        }
    }

    private object FakeFormElement : FormElement {
        override val identifier: IdentifierSpec
            get() = throw IllegalStateException("Should not be retrieved!")

        override val controller: Controller
            get() = throw IllegalStateException("Should not be retrieved!")

        override val allowsUserInteraction: Boolean = true
        override val mandateText: ResolvableString? = null

        override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
            return stateFlowOf(emptyList())
        }
    }
}
