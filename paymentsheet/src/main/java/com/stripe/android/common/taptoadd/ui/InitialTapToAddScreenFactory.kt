package com.stripe.android.common.taptoadd.ui

import javax.inject.Inject

internal class InitialTapToAddScreenFactory @Inject constructor(
    private val paymentMethodHolder: TapToAddStateHolder,
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
    private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
    private val cardAddedInteractorFactory: TapToAddCardAddedInteractor.Factory,
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return when (val state = paymentMethodHolder.state) {
            is TapToAddStateHolder.State.CardAdded -> {
                TapToAddNavigator.Screen.CardAdded(
                    interactor = cardAddedInteractorFactory.create(state.paymentMethod)
                )
            }
            is TapToAddStateHolder.State.Confirmation -> {
                TapToAddNavigator.Screen.Confirmation(
                    interactor = confirmationInteractorFactory.create(
                        paymentMethod = state.paymentMethod,
                        linkInput = state.linkInput,
                    )
                )
            }
            null -> {
                TapToAddNavigator.Screen.Collecting(
                    interactor = collectingInteractorFactory.create()
                )
            }
        }
    }
}
