package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod

@OptIn(ExperimentalCustomerSheetApi::class)
internal class FakeCustomerAdapter(
    private val paymentMethods: Result<List<PaymentMethod>> = Result.success(listOf()),
    private val selectedPaymentOption: Result<CustomerAdapter.PaymentOption?> = Result.success(null),
) : CustomerAdapter {
    override suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>> {
        return paymentMethods
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun setSelectedPaymentOption(
        paymentOption: CustomerAdapter.PaymentOption?
    ): Result<CustomerAdapter.PaymentOption?> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSelectedPaymentOption(): Result<CustomerAdapter.PaymentOption?> {
        return selectedPaymentOption
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): Result<String> {
        TODO("Not yet implemented")
    }
}
