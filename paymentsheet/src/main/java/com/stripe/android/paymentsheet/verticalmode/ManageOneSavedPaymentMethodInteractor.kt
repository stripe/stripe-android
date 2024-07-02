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

    constructor(sheetViewModel: BaseSheetViewModel) : this(
        paymentMethod = sheetViewModel.paymentMethods.value.first(),
        paymentMethodMetadata = sheetViewModel.paymentMethodMetadata.value!!,
        providePaymentMethodName = sheetViewModel::providePaymentMethodName,
        onDeletePaymentMethod = { sheetViewModel.removePaymentMethod(it.id) },
        navigateBack = sheetViewModel::handleBackPressed,
    )

    override val state = ManageOneSavedPaymentMethodInteractor.State(
        paymentMethod.toDisplayableSavedPaymentMethod(
            providePaymentMethodName,
            paymentMethodMetadata,
        )
    )

    override fun handleViewAction(viewAction: ManageOneSavedPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            is ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod -> {
                onDeletePaymentMethod(state.paymentMethod.paymentMethod)
                navigateBack()
            }
        }
    }
}
