package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState

internal fun createCustomerState(
    paymentMethods: List<PaymentMethod>,
    isRemoveEnabled: Boolean,
    canRemoveLastPaymentMethod: Boolean,
): CustomerState {
    return CustomerState(
        id = "cus_1",
        ephemeralKeySecret = "ek_1",
        customerSessionClientSecret = "cuss_123",
        paymentMethods = paymentMethods,
        permissions = CustomerState.Permissions(
            canRemovePaymentMethods = isRemoveEnabled,
            canRemoveDuplicates = true,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
        ),
        defaultPaymentMethodId = null,
    )
}
