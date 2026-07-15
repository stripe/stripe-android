package com.stripe.android.checkout.ece

internal object ExpressCheckoutElementInteractorStateFactory {
    fun create(
        expressButtons: List<ExpressButton> = listOf(
            ExpressButton.Link,
            ExpressButton.GooglePay,
        ),
    ): ExpressCheckoutElementInteractor.State {
        return ExpressCheckoutElementInteractor.State(
            expressButtons = expressButtons,
        )
    }
}
