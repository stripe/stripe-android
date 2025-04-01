package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand

internal fun UpdatePaymentMethodInteractor.editCardDetailsInteractorHelper(
    block: EditCardDetailsInteractor.() -> Unit = {}
): EditCardDetailsInteractor {
    return editCardDetailsInteractor().apply(block)
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
