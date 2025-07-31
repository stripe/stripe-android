package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.elements.CustomerSessionApiPreview
import javax.inject.Inject

@OptIn(CustomerSessionApiPreview::class)
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
