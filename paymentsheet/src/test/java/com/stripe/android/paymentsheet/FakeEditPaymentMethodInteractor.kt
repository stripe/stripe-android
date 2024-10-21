package com.stripe.android.paymentsheet

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewState
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemoveOperation
import com.stripe.android.paymentsheet.ui.PaymentMethodUpdateOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeEditPaymentMethodInteractor(
    paymentMethod: PaymentMethod,
    override val isLiveMode: Boolean = true,
) : ModifiableEditPaymentMethodViewInteractor {
    override val viewState: StateFlow<EditPaymentMethodViewState> = MutableStateFlow(
        EditPaymentMethodViewState(
            status = EditPaymentMethodViewState.Status.Idle,
            last4 = paymentMethod.card?.last4 ?: "",
            canUpdate = false,
            availableBrands = paymentMethod.card?.networks?.available?.map { code ->
                EditPaymentMethodViewState.CardBrandChoice(CardBrand.fromCode(code))
            } ?: listOf(),
            selectedBrand = paymentMethod.card?.networks?.preferred?.let { code ->
                EditPaymentMethodViewState.CardBrandChoice(CardBrand.fromCode(code))
            } ?: EditPaymentMethodViewState.CardBrandChoice(CardBrand.Unknown),
            displayName = "Card".resolvableString,
            canRemove = true,
        )
    )

    override fun handleViewAction(viewAction: EditPaymentMethodViewAction) {
        // No-op
    }

    override fun close() {
        // No-op
    }

    class Factory : ModifiableEditPaymentMethodViewInteractor.Factory {
        private val _calls = Turbine<Call>()

        val calls: ReceiveTurbine<Call> = _calls

        override fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: ResolvableString,
            canRemove: Boolean,
            isLiveMode: Boolean,
            cardBrandFilter: CardBrandFilter
        ): ModifiableEditPaymentMethodViewInteractor {
            _calls.add(Call(initialPaymentMethod, canRemove))

            return FakeEditPaymentMethodInteractor(initialPaymentMethod, canRemove)
        }

        data class Call(
            val initialPaymentMethod: PaymentMethod,
            val canRemove: Boolean,
        )
    }
}
