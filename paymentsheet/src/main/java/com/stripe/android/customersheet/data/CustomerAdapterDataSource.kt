package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.map
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.SavedSelection
import javax.inject.Inject

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerAdapterDataSource @Inject constructor(
    private val customerAdapter: CustomerAdapter,
) : CustomerSheetCombinedDataSource {
    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return customerAdapter.retrievePaymentMethods().toCustomerSheetDataResult()
    }

    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        return customerAdapter.retrieveSelectedPaymentOption().map { result ->
            result?.toSavedSelection()
        }.toCustomerSheetDataResult()
    }
}
