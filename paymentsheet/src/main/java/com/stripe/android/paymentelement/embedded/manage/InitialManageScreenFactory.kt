package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import javax.inject.Inject

internal class InitialManageScreenFactory @Inject constructor(
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val manageInteractorFactory: EmbeddedManageScreenInteractorFactory,
) {
    fun createInitialScreen(): EmbeddedNavigator.Screen {
        val paymentMethods = customerStateHolder.customer.value?.paymentMethods
        return if (paymentMethods?.size == 1) {
            val paymentMethod = paymentMethods.first()
            EmbeddedNavigator.Screen.ManageUpdate(
                interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                    displayableSavedPaymentMethod = paymentMethod.toDisplayableSavedPaymentMethod(
                        paymentMethodMetadata = paymentMethodMetadata,
                        defaultPaymentMethodId = null,
                    )
                )
            )
        } else {
            EmbeddedNavigator.Screen.ManageAll(interactor = manageInteractorFactory.createManageScreenInteractor())
        }
    }
}
