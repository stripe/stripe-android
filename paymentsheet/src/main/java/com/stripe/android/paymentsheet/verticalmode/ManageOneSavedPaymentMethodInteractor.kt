package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal interface ManageOneSavedPaymentMethodInteractor {
    val state: State

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val paymentMethod: DisplayableSavedPaymentMethod,
        val isLiveMode: Boolean,
    )

    sealed class ViewAction {
        data object DeletePaymentMethod : ViewAction()
    }
}

internal class DefaultManageOneSavedPaymentMethodInteractor(
    paymentMethod: PaymentMethod,
    paymentMethodMetadata: PaymentMethodMetadata,
    providePaymentMethodName: (PaymentMethodCode?) -> String,
    private val onDeletePaymentMethod: (PaymentMethod) -> Unit,
    private val navigateBack: () -> Unit,
) : ManageOneSavedPaymentMethodInteractor {
    override val state = ManageOneSavedPaymentMethodInteractor.State(
        paymentMethod = paymentMethod.toDisplayableSavedPaymentMethod(
            providePaymentMethodName,
            paymentMethodMetadata,
        ),
        isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
    )

    override fun handleViewAction(viewAction: ManageOneSavedPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            is ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod -> {
                onDeletePaymentMethod(state.paymentMethod.paymentMethod)
                navigateBack()
            }
        }
    }

    companion object {
        fun create(sheetViewModel: BaseSheetViewModel): ManageOneSavedPaymentMethodInteractor {
            return DefaultManageOneSavedPaymentMethodInteractor(
                paymentMethod = sheetViewModel.savedPaymentMethodMutator.paymentMethods.value.first(),
                paymentMethodMetadata = sheetViewModel.paymentMethodMetadata.value!!,
                providePaymentMethodName = sheetViewModel::providePaymentMethodName,
                onDeletePaymentMethod = sheetViewModel.savedPaymentMethodMutator::removePaymentMethod,
                navigateBack = sheetViewModel::handleBackPressed,
            )
        }
    }
}
