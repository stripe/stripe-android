package com.stripe.android.customersheet.utils

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi

@OptIn(ExperimentalCustomerSheetApi::class, ExperimentalCustomerSessionApi::class)
class FakeCustomerSessionProvider : CustomerSheet.CustomerSessionProvider() {
    override suspend fun provideSetupIntentClientSecret(customerId: String): Result<String> {
        throw NotImplementedError("Not implemented yet!")
    }

    override suspend fun providesCustomerSessionClientSecret(): Result<CustomerSheet.CustomerSessionClientSecret> {
        throw NotImplementedError("Not implemented yet!")
    }
}
