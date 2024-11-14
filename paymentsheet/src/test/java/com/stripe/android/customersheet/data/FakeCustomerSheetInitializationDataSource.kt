package com.stripe.android.customersheet.data

internal class FakeCustomerSheetInitializationDataSource(
    private val onLoadCustomerSheetSession: () -> CustomerSheetDataResult<CustomerSheetSession>,
) : CustomerSheetInitializationDataSource {
    override suspend fun loadCustomerSheetSession(): CustomerSheetDataResult<CustomerSheetSession> {
        return onLoadCustomerSheetSession()
    }
}
