package com.stripe.android.paymentsheet.ui

internal sealed interface EditPaymentMethodViewAction {
    object OnBrandChoiceOptionsShown : EditPaymentMethodViewAction

    object OnBrandChoiceOptionsDismissed : EditPaymentMethodViewAction

    data class OnBrandChoiceChanged(
        val choice: CardBrandChoice
    ) : EditPaymentMethodViewAction

    object OnRemovePressed : EditPaymentMethodViewAction

    object OnRemoveConfirmed : EditPaymentMethodViewAction

    object OnUpdatePressed : EditPaymentMethodViewAction

    object OnRemoveConfirmationDismissed : EditPaymentMethodViewAction
}
