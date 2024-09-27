package com.stripe.android.customersheet.data

import javax.inject.Inject

internal class CustomerSessionInitializationDataSource @Inject constructor() : CustomerSheetInitializationDataSource {
    override suspend fun loadCustomerSheetSession(): CustomerSheetDataResult<CustomerSheetSession> {
        throw NotImplementedError("Not implemented yet!")
    }
}
