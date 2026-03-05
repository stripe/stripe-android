package com.stripe.android.common.taptoadd.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import javax.inject.Inject

internal class InitialTapToAddScreenFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val paymentMethodHolder: TapToAddPaymentMethodHolder,
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
    private val collectCvcInteractorFactory: TapToAddCollectCvcInteractor.Factory,
    private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return paymentMethodHolder.paymentMethod?.let {
            if (requiresTapToAddCvcCollection(paymentMethodMetadata, it)) {
                TapToAddNavigator.Screen.CollectCvc(
                    interactor = collectCvcInteractorFactory.create(it)
                )
            } else {
                TapToAddNavigator.Screen.Confirmation(
                    interactor = confirmationInteractorFactory.create(
                        paymentMethod = it,
                        paymentMethodOptionsParams = null,
                    )
                )
            }
        } ?: run {
            TapToAddNavigator.Screen.Collecting(
                interactor = collectingInteractorFactory.create()
            )
        }
    }
}
