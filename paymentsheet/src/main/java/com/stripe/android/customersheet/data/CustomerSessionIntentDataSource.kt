package com.stripe.android.customersheet.data

import javax.inject.Inject

internal class CustomerSessionIntentDataSource @Inject constructor() : CustomerSheetIntentDataSource {
    override val canCreateSetupIntents: Boolean = true

    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> {
        throw NotImplementedError("Not implemented yet!")
    }
}
