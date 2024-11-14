package com.stripe.android.customersheet.data

internal interface CustomerSheetIntentDataSource {
    val canCreateSetupIntents: Boolean

    suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String>
}
