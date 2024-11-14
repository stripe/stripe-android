package com.stripe.android.customersheet.data

internal interface CustomerSheetInitializationDataSource {
    suspend fun loadCustomerSheetSession(): CustomerSheetDataResult<CustomerSheetSession>
}
