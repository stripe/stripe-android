package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

internal interface CustomerRepository {
    suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod>

    suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod?
}
