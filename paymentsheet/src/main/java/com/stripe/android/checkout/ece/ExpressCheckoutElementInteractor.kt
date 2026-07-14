package com.stripe.android.checkout.ece

import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    data class State(
        val expressButtons: List<ExpressButton>,
    )
}

internal class DefaultExpressCheckoutElementInteractor(
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(
        ExpressCheckoutElementInteractor.State(
            expressButtons = listOf(
                ExpressButton.Link,
                ExpressButton.GooglePay,
            )
        )
    ),
) : ExpressCheckoutElementInteractor
