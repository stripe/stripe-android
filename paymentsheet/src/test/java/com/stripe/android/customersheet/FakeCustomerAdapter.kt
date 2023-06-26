package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod

@OptIn(ExperimentalCustomerSheetApi::class)
internal class FakeCustomerAdapter(
    var selectedPaymentOption: Result<CustomerAdapter.PaymentOption?> = Result.success(null),
    private val paymentMethods: Result<List<PaymentMethod>> = Result.success(listOf()),
    private val onSetSelectedPaymentOption: ((paymentOption: CustomerAdapter.PaymentOption?) -> Result<Unit>)? = null,
    private val onDetachPaymentMethod: ((paymentMethodId: String) -> Result<PaymentMethod>)? = null,
) : CustomerAdapter {
    override suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>> {
        return paymentMethods
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        return onDetachPaymentMethod?.invoke(paymentMethodId)
            ?: Result.success(paymentMethods.getOrNull()?.find { it.id!! == paymentMethodId }!!)
    }

    override suspend fun setSelectedPaymentOption(
        paymentOption: CustomerAdapter.PaymentOption?
    ): Result<Unit> {
        return onSetSelectedPaymentOption?.invoke(paymentOption) ?: run {
            selectedPaymentOption = Result.success(paymentOption)
            Result.success(Unit)
        }
    }

    override suspend fun retrieveSelectedPaymentOption(): Result<CustomerAdapter.PaymentOption?> {
        return selectedPaymentOption
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): Result<String> {
        TODO("Not yet implemented")
    }
}
