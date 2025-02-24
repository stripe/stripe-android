package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import javax.inject.Inject

internal class InitialManageScreenFactory @Inject constructor(
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val manageInteractorFactory: EmbeddedManageScreenInteractorFactory,
) {
    fun createInitialScreen(): ManageNavigator.Screen {
        val paymentMethods = customerStateHolder.customer.value?.paymentMethods
        return if (paymentMethods?.size == 1) {
            val paymentMethod = paymentMethods.first()
            val displayName = paymentMethod.type?.code?.let { code ->
                paymentMethodMetadata.supportedPaymentMethodForCode(code)
            }?.displayName.orEmpty()
            val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
                displayName = displayName,
                paymentMethod = paymentMethod,
                isCbcEligible = paymentMethodMetadata.cbcEligibility is CardBrandChoiceEligibility.Eligible,
            )
            ManageNavigator.Screen.Update(
                interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                    displayableSavedPaymentMethod = displayableSavedPaymentMethod
                )
            )
        } else {
            ManageNavigator.Screen.All(interactor = manageInteractorFactory.createManageScreenInteractor())
        }
    }
}
