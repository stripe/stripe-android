package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.coroutines.flow.StateFlow

internal interface CustomerStateHolder {
    val customer: StateFlow<CustomerState?>

    val paymentMethods: StateFlow<List<PaymentMethod>>

    val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>

    val canRemoveDuplicate: StateFlow<Boolean>

    val canRemove: StateFlow<Boolean>

    val canUpdateFullPaymentMethodDetails: StateFlow<Boolean>

    fun setCustomerState(customerState: CustomerState?)

    fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?)

    fun addPaymentMethod(paymentMethod: PaymentMethod)

    fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?)
}
