package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.repositories.CustomerRepository

internal class FakeCustomerRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList()
) : CustomerRepository {
    lateinit var savedPaymentMethod: PaymentMethod
    var error: Throwable? = null
    override suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod> = paymentMethods

    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod? = null
}
