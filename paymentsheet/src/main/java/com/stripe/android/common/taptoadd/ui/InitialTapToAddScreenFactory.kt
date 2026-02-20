package com.stripe.android.common.taptoadd.ui

import javax.inject.Inject

internal class InitialTapToAddScreenFactory @Inject constructor(
    private val paymentMethodHolder: TapToAddPaymentMethodHolder,
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
    private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return paymentMethodHolder.paymentMethod?.let {
            TapToAddNavigator.Screen.Confirmation(
                interactor = confirmationInteractorFactory.create(it)
            )
        } ?: run {
            TapToAddNavigator.Screen.Collecting(
                interactor = collectingInteractorFactory.create()
            )
        }
    }
}
