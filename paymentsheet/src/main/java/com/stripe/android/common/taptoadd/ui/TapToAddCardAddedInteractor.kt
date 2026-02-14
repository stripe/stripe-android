package com.stripe.android.common.taptoadd.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddCardAddedInteractor {
    val cardBrand: CardBrand
    val last4: String?

    fun onShown()

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddCardAddedInteractor
    }
}

internal class DefaultTapToAddCardAddedInteractor(
    private val paymentMethod: PaymentMethod,
    private val onShown: () -> Unit,
) : TapToAddCardAddedInteractor {
    override val cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown
    override val last4 = paymentMethod.card?.last4

    override fun onShown() {
        onShown.invoke()
    }

    class Factory @Inject constructor(
        private val navigator: Provider<TapToAddNavigator>,
        private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
    ) : TapToAddCardAddedInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddCardAddedInteractor {
            return DefaultTapToAddCardAddedInteractor(
                paymentMethod = paymentMethod,
                onShown = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.Confirmation(
                                interactor = confirmationInteractorFactory.create(paymentMethod),
                            )
                        )
                    )
                },
            )
        }
    }
}
