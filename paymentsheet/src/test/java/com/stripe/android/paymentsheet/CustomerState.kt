package com.stripe.android.paymentsheet

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.CustomerState

internal fun createCustomerState(
    paymentMethods: List<PaymentMethod>,
    defaultPaymentMethodId: String? = null,
): CustomerState {
    return CustomerState(
        customerMetadata = PaymentMethodMetadataFixtures.CUSTOMER_SESSIONS_CUSTOMER_METADATA,
        paymentMethods = paymentMethods,
        defaultPaymentMethodId = defaultPaymentMethodId,
    )
}
