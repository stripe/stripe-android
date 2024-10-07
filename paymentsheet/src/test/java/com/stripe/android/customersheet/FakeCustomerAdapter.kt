package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodUpdateParams

internal class FakeCustomerAdapter(
    override var canCreateSetupIntents: Boolean = true,
    override val paymentMethodTypes: List<String>? = null,
    var selectedPaymentOption: CustomerAdapter.Result<CustomerAdapter.PaymentOption?> =
        CustomerAdapter.Result.success(null),
    private val paymentMethods: CustomerAdapter.Result<List<PaymentMethod>> =
        CustomerAdapter.Result.success(listOf(CARD_PAYMENT_METHOD)),
    private val onPaymentMethods: (() -> CustomerAdapter.Result<List<PaymentMethod>>)? = {
        paymentMethods
    },
    private val onGetPaymentOption: (() -> CustomerAdapter.Result<CustomerAdapter.PaymentOption?>)? = {
        selectedPaymentOption
    },
    private val onSetSelectedPaymentOption:
    ((paymentOption: CustomerAdapter.PaymentOption?) -> CustomerAdapter.Result<Unit>)? = null,
    private val onAttachPaymentMethod: ((paymentMethodId: String) -> CustomerAdapter.Result<PaymentMethod>)? = null,
    private val onDetachPaymentMethod: ((paymentMethodId: String) -> CustomerAdapter.Result<PaymentMethod>)? = null,
    private val onUpdatePaymentMethod:
    ((paymentMethodId: String, params: PaymentMethodUpdateParams) -> CustomerAdapter.Result<PaymentMethod>)? = null,
    private val onSetupIntentClientSecretForCustomerAttach: (() -> CustomerAdapter.Result<String>)? = null
) : CustomerAdapter {

    override suspend fun retrievePaymentMethods(): CustomerAdapter.Result<List<PaymentMethod>> {
        return onPaymentMethods?.invoke() ?: paymentMethods
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerAdapter.Result<PaymentMethod> {
        return onAttachPaymentMethod?.invoke(paymentMethodId)
            ?: CustomerAdapter.Result.success(paymentMethods.getOrNull()?.find { it.id!! == paymentMethodId }!!)
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerAdapter.Result<PaymentMethod> {
        return onDetachPaymentMethod?.invoke(paymentMethodId)
            ?: CustomerAdapter.Result.success(paymentMethods.getOrNull()?.find { it.id!! == paymentMethodId }!!)
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): CustomerAdapter.Result<PaymentMethod> {
        return onUpdatePaymentMethod?.invoke(paymentMethodId, params)
            ?: CustomerAdapter.Result.success(
                paymentMethods.getOrNull()?.find {
                    it.id!! == paymentMethodId
                }!!
            )
    }

    override suspend fun setSelectedPaymentOption(
        paymentOption: CustomerAdapter.PaymentOption?
    ): CustomerAdapter.Result<Unit> {
        return onSetSelectedPaymentOption?.invoke(paymentOption) ?: run {
            selectedPaymentOption = CustomerAdapter.Result.success(paymentOption)
            CustomerAdapter.Result.success(Unit)
        }
    }

    override suspend fun retrieveSelectedPaymentOption(): CustomerAdapter.Result<CustomerAdapter.PaymentOption?> {
        return onGetPaymentOption?.invoke() ?: selectedPaymentOption
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): CustomerAdapter.Result<String> {
        return onSetupIntentClientSecretForCustomerAttach?.invoke()
            ?: CustomerAdapter.Result.failure(
                displayMessage = "Merchant provided error message",
                cause = Exception("Some error"),
            )
    }
}
