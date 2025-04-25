package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheetFixtures

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

internal fun UpdatePaymentMethodInteractor.cardParamsUpdateAction(
    cardBrand: CardBrand,
    expiryMonth: Int? = null,
    expiryYear: Int? = null,
    billingDetails: PaymentMethod.BillingDetails? = null
) {
    handleViewAction(
        viewAction = UpdatePaymentMethodInteractor.ViewAction.CardUpdateParamsChanged(
            cardUpdateParams = CardUpdateParams(
                cardBrand = cardBrand,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                billingDetails = billingDetails
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

internal fun EditCardDetailsInteractor.updateBillingDetails(
    billingDetailsFormState: BillingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState()
) {
    handleViewAction(
        viewAction = EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(
            billingDetailsFormState = billingDetailsFormState
        )
    )
}
