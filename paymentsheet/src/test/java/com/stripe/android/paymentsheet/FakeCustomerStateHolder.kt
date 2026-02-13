package com.stripe.android.paymentsheet

import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeCustomerStateHolder : CustomerStateHolder {
    val addPaymentMethodTurbine = Turbine<PaymentMethod>()

    val customerStateTurbine = Turbine<CustomerState?>()

    val defaultPaymentMethodTurbine = Turbine<PaymentMethod?>()

    val updateMostRecentlySelectedSavedPaymentMethodTurbine = Turbine<PaymentMethod?>()

    override val customer: StateFlow<CustomerState?>
        get() = stateFlowOf(null)

    override val paymentMethods: StateFlow<List<PaymentMethod>>
        get() = stateFlowOf(emptyList())

    override val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>
        get() = stateFlowOf(null)

    override val canRemoveDuplicate: StateFlow<Boolean>
        get() = stateFlowOf(false)

    override val canRemove: StateFlow<Boolean>
        get() = stateFlowOf(false)

    override val canUpdateFullPaymentMethodDetails: StateFlow<Boolean>
        get() = stateFlowOf(false)

    override fun setCustomerState(customerState: CustomerState?) {
        customerStateTurbine.add(customerState)
    }

    override fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?) {
        defaultPaymentMethodTurbine.add(paymentMethod)
    }

    override fun addPaymentMethod(paymentMethod: PaymentMethod) {
        addPaymentMethodTurbine.add(paymentMethod)
    }

    override fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        updateMostRecentlySelectedSavedPaymentMethodTurbine.add(paymentMethod)
    }
}
