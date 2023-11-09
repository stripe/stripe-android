package com.stripe.android.paymentsheet.ui

internal sealed interface EditPaymentMethodViewAction {
    data class OnBrandChoiceChanged(
        val choice: EditPaymentMethodViewState.CardBrandChoice
    ) : EditPaymentMethodViewAction

    object OnRemovePressed : EditPaymentMethodViewAction

    object OnUpdatePressed : EditPaymentMethodViewAction
}
