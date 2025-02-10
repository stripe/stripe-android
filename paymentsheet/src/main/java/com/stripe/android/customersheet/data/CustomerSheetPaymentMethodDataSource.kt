package com.stripe.android.customersheet.data

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams

/**
 * [CustomerSheetPaymentMethodDataSource] defines a set of operations for managing saved payment methods within a
 * [com.stripe.android.customersheet.CustomerSheet] context.
 */
internal interface CustomerSheetPaymentMethodDataSource {
    /**
     * Retrieves a list of payment methods
     *
     * @return a result containing the list of payment methods if operation was successful
     */
    suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>>

    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): CustomerSheetDataResult<PaymentMethod>

    suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod>

    suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod>

    // TODO: add set default payment method here. Failure result and unexpectedd error for CustomerAdapterDS.
}
