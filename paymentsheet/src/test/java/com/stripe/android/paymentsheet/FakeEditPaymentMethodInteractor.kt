package com.stripe.android.paymentsheet

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
    paymentMethod: PaymentMethod
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
            displayName = "Card",
        )
    )

    override fun handleViewAction(viewAction: EditPaymentMethodViewAction) {
        // No-op
    }

    override fun close() {
        // No-op
    }

    object Factory : ModifiableEditPaymentMethodViewInteractor.Factory {
        override fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: String
        ): ModifiableEditPaymentMethodViewInteractor {
            return FakeEditPaymentMethodInteractor(initialPaymentMethod)
        }
    }
}
