package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.CvcFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
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

internal class DefaultTapToAddCollectCvcInteractorTest {
    @Test
    fun `initial state is correct`() = runScenario(
        paymentMethod = PaymentMethodFactory.card().update(
            last4 = "1234",
            brand = CardBrand.MasterCard,
            addCbcNetworks = false,
        ),
        initialCvcState = CvcFormHelper.State.Incomplete,
    ) {
        val state = interactor.state.value
        assertThat(state.cardBrand).isEqualTo(CardBrand.MasterCard)
        assertThat(state.last4).isEqualTo("1234")
        assertThat(state.title).isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
        assertThat(state.primaryButton.label)
            .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
        assertThat(state.primaryButton.enabled).isFalse()
        assertThat(state.form.elements).containsExactly(fakeCvcFormHelper.formElement)
        assertThat(state.form.enabled).isTrue()
    }

    @Test
    fun `state title uses pay amount label in complete mode`() = runScenario(
        tapToAddMode = TapToAddMode.Complete,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 5099,
            ),
        ),
    ) {
        interactor.state.test {
            assertThat(awaitItem().title).isEqualTo(
                resolvableString(
                    StripeUiCoreR.string.stripe_pay_button_amount,
                    "$50.99",
                )
            )
        }
    }

    @Test
    fun `state primary button is disabled when cvc helper is incomplete`() = runScenario(
        initialCvcState = CvcFormHelper.State.Complete("123"),
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.enabled).isTrue()
            fakeCvcFormHelper.updateState(CvcFormHelper.State.Incomplete)
            assertThat(awaitItem().primaryButton.enabled).isFalse()
        }
    }

    @Test
    fun `state primary button is enabled when cvc helper emits complete state`() = runScenario(
        initialCvcState = CvcFormHelper.State.Incomplete,
    ) {
        interactor.state.test {
            assertThat(awaitItem().primaryButton.enabled).isFalse()
            fakeCvcFormHelper.updateState(CvcFormHelper.State.Complete("123"))
            assertThat(awaitItem().primaryButton.enabled).isTrue()
        }
    }

    @Test
    fun `PrimaryButtonPressed calls onContinue with cvc when state is complete`() = runScenario(
        initialCvcState = CvcFormHelper.State.Complete("123"),
    ) {
        interactor.performAction(TapToAddCollectCvcInteractor.Action.PrimaryButtonPressed)

        assertThat(onContinueCalls.awaitItem())
            .isEqualTo(PaymentMethodOptionsParams.Card(cvc = "123"))
    }

    private fun runScenario(
        paymentMethod: PaymentMethod = PaymentMethodFactory.card(last4 = "4242"),
        tapToAddMode: TapToAddMode = TapToAddMode.Continue,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        initialCvcState: CvcFormHelper.State = CvcFormHelper.State.Incomplete,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onContinueCalls = Turbine<PaymentMethodOptionsParams.Card>()
        val fakeCvcFormHelper = FakeCvcFormHelper(initialState = initialCvcState)

        val scenario = Scenario(
            interactor = DefaultTapToAddCollectCvcInteractor(
                coroutineScope = backgroundScope,
                tapToAddMode = tapToAddMode,
                paymentMethod = paymentMethod,
                paymentMethodMetadata = paymentMethodMetadata,
                cvcFormHelper = fakeCvcFormHelper,
                onContinue = { onContinueCalls.add(it) },
            ),
            fakeCvcFormHelper = fakeCvcFormHelper,
            onContinueCalls = onContinueCalls,
        )
        block(scenario)

        onContinueCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val interactor: TapToAddCollectCvcInteractor,
        val fakeCvcFormHelper: FakeCvcFormHelper,
        val onContinueCalls: ReceiveTurbine<PaymentMethodOptionsParams.Card>,
    )

    private class FakeCvcFormHelper(
        initialState: CvcFormHelper.State,
    ) : CvcFormHelper {
        private val _state = MutableStateFlow(initialState)

        override val state: StateFlow<CvcFormHelper.State> = _state
        override val formElement: FormElement = FakeFormElement

        fun updateState(state: CvcFormHelper.State) {
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
