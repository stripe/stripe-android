package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState

internal fun createCustomerState(
    paymentMethods: List<PaymentMethod>,
    defaultPaymentMethodId: String? = null,
): CustomerState {
    return CustomerState(
        paymentMethods = paymentMethods,
        defaultPaymentMethodId = defaultPaymentMethodId,
    )
}
