package com.stripe.android.customersheet.data

internal interface CustomerSheetIntentDataSource {
    suspend fun canCreateSetupIntents(): CustomerSheetDataResult<Boolean>

    suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String>
}
