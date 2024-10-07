package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import javax.inject.Inject

@OptIn(ExperimentalCustomerSessionApi::class)
internal class CustomerSessionIntentDataSource @Inject constructor(
    private val elementsSessionManager: CustomerSessionElementsSessionManager,
    private val customerSessionProvider: CustomerSheet.CustomerSessionProvider,
) : CustomerSheetIntentDataSource {
    override val canCreateSetupIntents: Boolean = true

    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> {
        return elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
            customerSessionProvider.provideSetupIntentClientSecret(ephemeralKey.customerId).getOrThrow()
        }.toCustomerSheetDataResult()
    }
}
