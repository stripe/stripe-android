package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeTapToAddCardAddedInteractor(
    cardBrand: CardBrand = CardBrand.Visa,
    last4: String? = "4242",
    primaryButtonEnabled: Boolean = false,
) : TapToAddCardAddedInteractor {
    override val state: StateFlow<TapToAddCardAddedInteractor.State> = MutableStateFlow(
        TapToAddCardAddedInteractor.State(
            cardBrand = cardBrand,
            last4 = last4,
            title = "Card added".resolvableString,
            primaryButton = TapToAddCardAddedInteractor.State.PrimaryButton(
                label = "Continue".resolvableString,
                enabled = primaryButtonEnabled,
            ),
            form = TapToAddCardAddedInteractor.State.Form(
                elements = emptyList(),
                enabled = true,
            )
        )
    )

    private val _onClose = Turbine<Unit>()
    val onClose: ReceiveTurbine<Unit> = _onClose

    override fun performAction(action: TapToAddCardAddedInteractor.Action) {
        // No-op
    }

    override fun close() {
        _onClose.add(Unit)
    }

    fun validate() {
        _onClose.ensureAllEventsConsumed()
    }

    class Factory(
        val interactor: FakeTapToAddCardAddedInteractor = FakeTapToAddCardAddedInteractor()
    ) : TapToAddCardAddedInteractor.Factory {
        private val _createCalls = Turbine<PaymentMethod>()
        val createCalls: ReceiveTurbine<PaymentMethod> = _createCalls

        override fun create(
            paymentMethod: PaymentMethod,
        ): TapToAddCardAddedInteractor {
            _createCalls.add(paymentMethod)

            return interactor
        }

        fun validate() {
            _createCalls.ensureAllEventsConsumed()
        }
    }
}
