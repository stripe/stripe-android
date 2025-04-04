package com.stripe.android.paymentsheet.ui

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    override fun create(onCardUpdateParamsChanged: CardUpdateParamsCallback): EditCardDetailsInteractor {
        this.onCardUpdateParamsChanged = onCardUpdateParamsChanged
        return FakeEditCardDetailsInteractor()
    }
}
