package com.stripe.android.common.taptoadd.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject

internal class TapToAddScreenFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val paymentMethodHolder: TapToAddPaymentMethodHolder,
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
    private val collectCvcInteractorFactory: TapToAddCollectCvcInteractor.Factory,
    private val confirmationInteractorFactory: TapToAddConfirmationInteractor.Factory
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return paymentMethodHolder.paymentMethod?.let { paymentMethod ->
            createPostCollectionScreen(paymentMethod)
        } ?: run {
            TapToAddNavigator.Screen.Collecting(
                interactor = collectingInteractorFactory.create()
            )
        }
    }

    fun createPostCollectionScreen(paymentMethod: PaymentMethod): TapToAddNavigator.Screen {
        return if (requiresTapToAddCvcCollection(paymentMethodMetadata, paymentMethod)) {
            TapToAddNavigator.Screen.CollectCvc(
                interactor = collectCvcInteractorFactory.create(paymentMethod)
            )
        } else {
            TapToAddNavigator.Screen.Confirmation(
                interactor = confirmationInteractorFactory.create(
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = null,
                )
            )
        }
    }
}
