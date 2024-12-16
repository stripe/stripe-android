package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerSheet

internal interface CustomerSheetInitializationDataSource {
    suspend fun loadCustomerSheetSession(
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetDataResult<CustomerSheetSession>
}
