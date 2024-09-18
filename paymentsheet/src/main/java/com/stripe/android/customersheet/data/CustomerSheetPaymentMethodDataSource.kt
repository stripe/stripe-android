package com.stripe.android.customersheet.data

import com.stripe.android.model.PaymentMethod

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
}
