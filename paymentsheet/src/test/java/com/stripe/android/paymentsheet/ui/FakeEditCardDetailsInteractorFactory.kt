package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod

internal class FakeEditCardDetailsInteractorFactory(
    private val shouldShowCardBrandDropdown: Boolean = true
) : EditCardDetailsInteractor.Factory {
    override fun create(
        card: PaymentMethod.Card,
        onCardUpdateParamsChanged: CardUpdateParamsCallback
    ): EditCardDetailsInteractor {
        return FakeEditCardDetailsInteractor(
            card = card,
            shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
            onCardUpdateParamsChanged = onCardUpdateParamsChanged
        )
    }
}
