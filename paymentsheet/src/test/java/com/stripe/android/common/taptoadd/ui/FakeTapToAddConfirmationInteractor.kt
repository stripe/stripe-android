package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeTapToAddConfirmationInteractor(
    showCvcElement: Boolean = false,
    cvcInitialValue: String? = null,
    cardBrand: CardBrand = CardBrand.Visa,
    last4: String? = "4242",
    locked: Boolean = true,
    primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State =
        TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
    error: ResolvableString? = null,
) : TapToAddConfirmationInteractor {
    override val state: StateFlow<TapToAddConfirmationInteractor.State> = MutableStateFlow(
        TapToAddConfirmationInteractor.State(
            cardBrand = cardBrand,
            last4 = last4,
            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                label = "Pay".resolvableString,
                locked = locked,
                state = primaryButtonState,
                enabled = true,
            ),
            form = TapToAddConfirmationInteractor.State.Form(
                elements = listOf(createCvcElement(cardBrand, cvcInitialValue)).takeIf {
                    showCvcElement
                } ?: emptyList(),
                enabled = true,
            ),
            error = error,
        )
    )

    private val _onClose = Turbine<Unit>()
    val onClose: ReceiveTurbine<Unit> = _onClose

    override fun performAction(action: TapToAddConfirmationInteractor.Action) {
        // No-op
    }

    override fun close() {
        _onClose.add(Unit)
    }

    fun validate() {
        _onClose.ensureAllEventsConsumed()
    }

    private fun createCvcElement(
        cardBrand: CardBrand,
        initialValue: String?,
    ): FormElement {
        val cvcController = CvcController(
            cardBrandFlow = stateFlowOf(cardBrand),
            initialValue = initialValue,
        )
        val cvcElement = CvcElement(
            _identifier = IdentifierSpec.CardCvc,
            controller = cvcController,
        )

        return SectionElement.wrap(
            sectionFieldElement = cvcElement,
            label = "Confirm your CVC".resolvableString,
        )
    }

    class Factory(
        val interactor: FakeTapToAddConfirmationInteractor = FakeTapToAddConfirmationInteractor()
    ) : TapToAddConfirmationInteractor.Factory {
        private val _createCalls = Turbine<Pair<PaymentMethod, UserInput?>>()
        val createCalls: ReceiveTurbine<Pair<PaymentMethod, UserInput?>> = _createCalls

        override fun create(
            paymentMethod: PaymentMethod,
            linkInput: UserInput?,
        ): TapToAddConfirmationInteractor {
            _createCalls.add(paymentMethod to linkInput)

            return interactor
        }

        fun validate() {
            _createCalls.ensureAllEventsConsumed()
        }
    }
}
