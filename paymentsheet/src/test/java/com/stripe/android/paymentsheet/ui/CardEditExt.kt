package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.CardUpdateParams

internal fun UpdatePaymentMethodInteractor.editCardDetailsInteractorHelper(
    block: EditCardDetailsInteractor.() -> Unit = {}
): EditCardDetailsInteractor {
    return editCardDetailsInteractor.apply(block)
}

internal fun EditCardDetailsInteractor.updateCardBrand(cardBrand: CardBrand) {
    handleViewAction(
        viewAction = EditCardDetailsInteractor.ViewAction.BrandChoiceChanged(
            cardBrandChoice = CardBrandChoice(
                brand = cardBrand,
                enabled = true
            )
        )
    )
}

internal fun UpdatePaymentMethodInteractor.cardParamsUpdateAction(cardBrand: CardBrand) {
    handleViewAction(
        viewAction = UpdatePaymentMethodInteractor.ViewAction.CardUpdateParamsChanged(
            cardUpdateParams = CardUpdateParams(
                cardBrand = cardBrand
            )
        )
    )
}

internal fun UpdatePaymentMethodInteractor.nullCardParamsUpdateAction() {
    handleViewAction(UpdatePaymentMethodInteractor.ViewAction.CardUpdateParamsChanged(null))
}

internal fun EditCardDetailsInteractor.updateExpiryDate(text: String) {
    handleViewAction(
        viewAction = EditCardDetailsInteractor.ViewAction.DateChanged(text)
    )
}
