package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages customer state and saved payment methods.
 */
internal interface CustomerStateHolder {
    val customer: StateFlow<CustomerState?>

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    val paymentMethods: StateFlow<List<PaymentMethod>>

    val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>

    val canRemoveDuplicate: StateFlow<Boolean>

    val canRemove: StateFlow<Boolean>

    val canUpdateFullPaymentMethodDetails: StateFlow<Boolean>

    fun setCustomerState(customerState: CustomerState?)

    fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?)

    fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?)

    fun addPaymentMethod(paymentMethod: PaymentMethod)

    companion object {
        const val SAVED_CUSTOMER = "customer_info"
        const val SAVED_PM_SELECTION = "saved_selection"
    }

    interface Factory {
        fun create(viewModel: BaseSheetViewModel): CustomerStateHolder
    }
}
