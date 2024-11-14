package com.stripe.android.customersheet.data

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodUpdateParams

private typealias AttachPaymentMethodOperation = (paymentMethodId: String) -> CustomerSheetDataResult<PaymentMethod>
private typealias DetachPaymentMethodOperation = (paymentMethodId: String) -> CustomerSheetDataResult<PaymentMethod>
private typealias UpdatePaymentMethodOperation =
    (paymentMethodId: String, params: PaymentMethodUpdateParams) -> CustomerSheetDataResult<PaymentMethod>

internal class FakeCustomerSheetPaymentMethodDataSource(
    private val paymentMethods: CustomerSheetDataResult<List<PaymentMethod>> =
        CustomerSheetDataResult.success(listOf(CARD_PAYMENT_METHOD)),
    private val onAttachPaymentMethod: AttachPaymentMethodOperation = { id ->
        CustomerSheetDataResult.success(findPaymentMethod(paymentMethods, id))
    },
    private val onDetachPaymentMethod: DetachPaymentMethodOperation = { id ->
        CustomerSheetDataResult.success(findPaymentMethod(paymentMethods, id))
    },
    private val onUpdatePaymentMethod: UpdatePaymentMethodOperation = { id, _ ->
        CustomerSheetDataResult.success(findPaymentMethod(paymentMethods, id))
    },
) : CustomerSheetPaymentMethodDataSource {
    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return paymentMethods
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): CustomerSheetDataResult<PaymentMethod> {
        return onUpdatePaymentMethod.invoke(paymentMethodId, params)
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return onAttachPaymentMethod.invoke(paymentMethodId)
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return onDetachPaymentMethod.invoke(paymentMethodId)
    }

    private companion object {
        fun findPaymentMethod(
            paymentMethods: CustomerSheetDataResult<List<PaymentMethod>>,
            id: String,
        ): PaymentMethod {
            return paymentMethods.fold(
                onSuccess = { it },
                onFailure = { _, _ -> listOf() }
            ).find { method ->
                method.id!! == id
            }!!
        }
    }
}
