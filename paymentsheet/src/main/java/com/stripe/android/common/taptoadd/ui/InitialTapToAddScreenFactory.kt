package com.stripe.android.common.taptoadd.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import javax.inject.Inject

internal class InitialTapToAddScreenFactory @Inject constructor(
    private val paymentMethodHolder: TapToAddPaymentMethodHolder,
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
    private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
    private val collectCvcInteractorFactory: TapToAddCollectCvcInteractor.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return paymentMethodHolder.paymentMethod?.let { paymentMethod ->
            if (requiresTapToAddCvcCollection(paymentMethodMetadata, paymentMethod)) {
                TapToAddNavigator.Screen.CollectCvc(
                    collectCvcInteractorFactory.create(paymentMethod)
                )
            } else {
                TapToAddNavigator.Screen.Confirmation(
                    interactor = confirmationInteractorFactory.create(
                        paymentMethod = paymentMethod,
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
