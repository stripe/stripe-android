package com.stripe.android.customersheet.data

import com.stripe.android.elements.customersheet.CustomerSheet

internal interface CustomerSheetInitializationDataSource {
    suspend fun loadCustomerSheetSession(
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetDataResult<CustomerSheetSession>
}
