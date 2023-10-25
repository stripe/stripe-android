package com.stripe.android.paymentsheet.ui

internal sealed interface EditPaymentMethodViewAction {
    data class OnBrandChoiceChanged(
        val choice: EditPaymentViewState.CardBrandChoice
    ) : EditPaymentMethodViewAction

    object OnRemovePressed : EditPaymentMethodViewAction

    object OnUpdatePressed : EditPaymentMethodViewAction
}
