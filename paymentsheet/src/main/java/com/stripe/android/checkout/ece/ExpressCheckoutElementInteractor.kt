package com.stripe.android.checkout.ece

import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    data class State(
        val walletButtons: List<ExpressButton>,
    )

    fun interface Factory {
        fun create(): ExpressCheckoutElementInteractor
    }

    sealed interface ExpressButton {
        data object Link : ExpressButton
        data object GooglePay : ExpressButton
    }
}

internal class DefaultExpressCheckoutElementInteractor(
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(
        ExpressCheckoutElementInteractor.State(walletButtons = emptyList())
    ),
) : ExpressCheckoutElementInteractor {
    internal object Factory : ExpressCheckoutElementInteractor.Factory {
        override fun create(): ExpressCheckoutElementInteractor {
            return DefaultExpressCheckoutElementInteractor()
        }
    }
}
