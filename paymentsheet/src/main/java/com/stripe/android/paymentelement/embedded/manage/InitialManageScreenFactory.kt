package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetScreen
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import javax.inject.Inject

internal class InitialManageScreenFactory @Inject constructor(
    private val customerStateHolder: CustomerStateHolder,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val manageInteractorFactory: EmbeddedManageScreenInteractorFactory,
) {
    fun createInitialScreen(
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
        close: (shouldInvokeRowSelectionCallback: Boolean) -> Unit,
        navigateBack: () -> Unit,
        canGoBack: () -> Boolean,
    ): EmbeddedSheetScreen {
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
            EmbeddedSheetScreen.ManageUpdate(
                interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                    displayableSavedPaymentMethod = displayableSavedPaymentMethod,
                    savedPaymentMethodMutator = savedPaymentMethodMutator,
                    navigateBack = navigateBack,
                ),
                canGoBack = canGoBack,
                onBack = navigateBack,
            )
        } else {
            EmbeddedSheetScreen.ManageAll(
                interactor = manageInteractorFactory.createManageScreenInteractor(
                    savedPaymentMethodMutator = savedPaymentMethodMutator,
                    close = close,
                    navigateBack = navigateBack,
                ),
                canGoBack = canGoBack,
                onBack = navigateBack,
            )
        }
    }
}
