package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod

internal interface UpdatePaymentMethodInteractor {
    val isLiveMode: Boolean
    val canRemove: Boolean
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    val paymentMethod: UpdateablePaymentMethod

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
    }
}

internal sealed interface UpdateablePaymentMethod {
    data class Card(val card: PaymentMethod.Card): UpdateablePaymentMethod
    data class UsBankAccount(val usBankAccount: PaymentMethod.USBankAccount): UpdateablePaymentMethod
    data class SepaDebit(val sepaDebit: PaymentMethod.SepaDebit): UpdateablePaymentMethod
}

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val paymentMethod: UpdateablePaymentMethod,
    val onRemovePaymentMethod: (PaymentMethod) -> Unit,
    val navigateBack: () -> Unit,
) : UpdatePaymentMethodInteractor {
    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> {
                onRemovePaymentMethod(displayableSavedPaymentMethod.paymentMethod)
                navigateBack()
            }
        }
    }
}
