package com.stripe.android.common.taptoadd.ui

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddDelayInteractor {
    val cardBrand: CardBrand
    val last4: String?

    interface Factory {
        fun create(
            paymentMethod: PaymentMethod,
            delayedScreen: TapToAddNavigator.Screen,
        ): TapToAddDelayInteractor
    }
}

internal class DefaultTapToAddDelayInteractor(
    private val coroutineScope: CoroutineScope,
    private val paymentMethod: PaymentMethod,
    private val onShown: () -> Unit,
) : TapToAddDelayInteractor {
    override val cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown
    override val last4 = paymentMethod.card?.last4

    init {
        coroutineScope.launch {
            delay(SHOWN_SCREEN_DELAY)
            onShown()
        }
    }

    class Factory @Inject constructor(
        @ViewModelScope private val coroutineScope: CoroutineScope,
        private val navigator: Provider<TapToAddNavigator>,
    ) : TapToAddDelayInteractor.Factory {
        override fun create(
            paymentMethod: PaymentMethod,
            delayedScreen: TapToAddNavigator.Screen,
        ): TapToAddDelayInteractor {
            return DefaultTapToAddDelayInteractor(
                coroutineScope = coroutineScope,
                paymentMethod = paymentMethod,
                onShown = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(delayedScreen),
                    )
                },
            )
        }
    }

    private companion object {
        const val SHOWN_SCREEN_DELAY = 1000L
    }
}
