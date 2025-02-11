package com.stripe.android.customersheet.utils

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi

@OptIn(ExperimentalCustomerSessionApi::class)
class FakeCustomerSessionProvider(
    private val onIntentConfiguration: () -> Result<CustomerSheet.IntentConfiguration> = {
        throw NotImplementedError("Not implemented yet!")
    },
    private val onProvideSetupIntentClientSecret: (String) -> Result<String> = {
        throw NotImplementedError("Not implemented yet!")
    },
    private val onProvidesCustomerSessionClientSecret: () -> Result<CustomerSheet.CustomerSessionClientSecret> = {
        throw NotImplementedError("Not implemented yet!")
    }
) : CustomerSheet.CustomerSessionProvider() {
    override suspend fun intentConfiguration(): Result<CustomerSheet.IntentConfiguration> {
        return onIntentConfiguration()
    }

    override suspend fun provideSetupIntentClientSecret(customerId: String): Result<String> {
        return onProvideSetupIntentClientSecret(customerId)
    }

    override suspend fun providesCustomerSessionClientSecret(): Result<CustomerSheet.CustomerSessionClientSecret> {
        return onProvidesCustomerSessionClientSecret()
    }
}
