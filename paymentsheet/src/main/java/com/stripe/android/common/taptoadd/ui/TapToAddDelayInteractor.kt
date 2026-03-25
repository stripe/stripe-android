package com.stripe.android.common.taptoadd.ui

import com.stripe.android.core.injection.UIContext
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface TapToAddDelayInteractor {
    val cardBrand: CardBrand
    val last4: String?

    fun close()

    interface Factory {
        fun create(
            paymentMethod: PaymentMethod,
            delayedScreen: TapToAddNavigator.Screen,
        ): TapToAddDelayInteractor
    }
}

internal class DefaultTapToAddDelayInteractor(
    coroutineContext: CoroutineContext,
    private val paymentMethod: PaymentMethod,
    private val onShown: () -> Unit,
) : TapToAddDelayInteractor {
    override val cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown
    override val last4 = paymentMethod.card?.last4

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    init {
        coroutineScope.launch {
            delay(SHOWN_SCREEN_DELAY)
            onShown()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    class Factory @Inject constructor(
        @UIContext private val coroutineContext: CoroutineContext,
        private val navigator: Provider<TapToAddNavigator>,
    ) : TapToAddDelayInteractor.Factory {
        override fun create(
            paymentMethod: PaymentMethod,
            delayedScreen: TapToAddNavigator.Screen,
        ): TapToAddDelayInteractor {
            return DefaultTapToAddDelayInteractor(
                coroutineContext = coroutineContext,
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
