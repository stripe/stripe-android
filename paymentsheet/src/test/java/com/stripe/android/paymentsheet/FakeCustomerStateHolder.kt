package com.stripe.android.paymentsheet

import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeCustomerStateHolder : CustomerStateHolder {
    override val customer: StateFlow<CustomerState?>
        get() = stateFlowOf<CustomerState?>(null)

    override val paymentMethods: StateFlow<List<PaymentMethod>>
        get() = stateFlowOf(emptyList())

    override val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>
        get() = stateFlowOf<PaymentMethod?>(null)

    override val canRemoveDuplicate: StateFlow<Boolean>
        get() = stateFlowOf(false)

    override val canRemove: StateFlow<Boolean>
        get() = stateFlowOf(false)

    override val canUpdateFullPaymentMethodDetails: StateFlow<Boolean>
        get() = stateFlowOf(false)

    private val setCustomerStateCalls = Turbine<CustomerState?>()

    private val setDefaultPaymentMethodCalls = Turbine<PaymentMethod?>()

    private val updateMostRecentlySelectedSavedPaymentMethodCalls = Turbine<PaymentMethod?>()

    override fun setCustomerState(customerState: CustomerState?) {
        setCustomerStateCalls.add(customerState)
    }

    override fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?) {
        setDefaultPaymentMethodCalls.add(paymentMethod)
    }

    override fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        updateMostRecentlySelectedSavedPaymentMethodCalls.add(paymentMethod)
    }

    fun validate() {
        setCustomerStateCalls.ensureAllEventsConsumed()
        setDefaultPaymentMethodCalls.ensureAllEventsConsumed()
        updateMostRecentlySelectedSavedPaymentMethodCalls.ensureAllEventsConsumed()
    }
}
