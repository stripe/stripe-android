package com.stripe.android.utils

import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerRepository

internal class FakeCustomerRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList(),
    private val customer: Customer? = CustomerFixtures.CUSTOMER,
    private val onDetachPaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onAttachPaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
) : CustomerRepository {
    lateinit var savedPaymentMethod: PaymentMethod
    var error: Throwable? = null

    override suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String
    ): Customer? = customer

    override suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod> = paymentMethods

    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod> = onDetachPaymentMethod()

    override suspend fun attachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod> = onAttachPaymentMethod()
}
