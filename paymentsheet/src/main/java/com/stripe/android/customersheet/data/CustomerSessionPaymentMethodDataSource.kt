package com.stripe.android.customersheet.data

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import javax.inject.Inject

internal class CustomerSessionPaymentMethodDataSource @Inject constructor() : CustomerSheetPaymentMethodDataSource {
    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        throw NotImplementedError("Not implemented yet!")
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): CustomerSheetDataResult<PaymentMethod> {
        throw NotImplementedError("Not implemented yet!")
    }

    override suspend fun attachPaymentMethod(
        paymentMethodId: String
    ): CustomerSheetDataResult<PaymentMethod> {
        throw NotImplementedError("Not implemented yet!")
    }

    override suspend fun detachPaymentMethod(
        paymentMethodId: String
    ): CustomerSheetDataResult<PaymentMethod> {
        throw NotImplementedError("Not implemented yet!")
    }
}
