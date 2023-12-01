package com.stripe.android.utils

import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerRepository

internal open class FakeCustomerRepository(
    private val paymentMethods: List<PaymentMethod> = emptyList(),
    private val customer: Customer? = null,
    private val onDetachPaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onAttachPaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    },
    private val onUpdatePaymentMethod: () -> Result<PaymentMethod> = {
        Result.failure(NotImplementedError())
    }
) : CustomerRepository {
    lateinit var savedPaymentMethod: PaymentMethod
    var error: Throwable? = null

    override suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String
    ): Customer? = customer

    override suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>> = Result.success(paymentMethods)

    override suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod> = onDetachPaymentMethod()

    override suspend fun attachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod> = onAttachPaymentMethod()

    override suspend fun updatePaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod> = onUpdatePaymentMethod()
}
