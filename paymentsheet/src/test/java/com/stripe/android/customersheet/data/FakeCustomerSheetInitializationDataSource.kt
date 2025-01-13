package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerSheet

internal class FakeCustomerSheetInitializationDataSource(
    private val onLoadCustomerSheetSession: (CustomerSheet.Configuration) ->
    CustomerSheetDataResult<CustomerSheetSession>,
) : CustomerSheetInitializationDataSource {
    override suspend fun loadCustomerSheetSession(
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetDataResult<CustomerSheetSession> {
        return onLoadCustomerSheetSession(configuration)
    }
}
