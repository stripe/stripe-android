package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
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
    providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
    private val onDeletePaymentMethod: (PaymentMethod) -> Unit,
    private val navigateBack: () -> Unit,
    private val defaultPaymentMethodId: String?,
) : ManageOneSavedPaymentMethodInteractor {
    override val state = ManageOneSavedPaymentMethodInteractor.State(
        paymentMethod = paymentMethod.toDisplayableSavedPaymentMethod(
            providePaymentMethodName,
            paymentMethodMetadata,
            defaultPaymentMethodId
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
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            savedPaymentMethodMutator: SavedPaymentMethodMutator,
        ): ManageOneSavedPaymentMethodInteractor {
            val defaultPaymentMethodId = savedPaymentMethodMutator.defaultPaymentMethodId.value
                ?: customerStateHolder.customer.value?.defaultPaymentMethodId
            return DefaultManageOneSavedPaymentMethodInteractor(
                paymentMethod = customerStateHolder.paymentMethods.value.first(),
                paymentMethodMetadata = paymentMethodMetadata,
                providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
                onDeletePaymentMethod = savedPaymentMethodMutator::removePaymentMethod,
                navigateBack = viewModel::handleBackPressed,
                defaultPaymentMethodId = defaultPaymentMethodId
            )
        }
    }
}
