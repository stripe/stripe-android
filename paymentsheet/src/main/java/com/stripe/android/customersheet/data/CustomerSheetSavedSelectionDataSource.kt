package com.stripe.android.customersheet.data

import com.stripe.android.paymentsheet.model.SavedSelection

internal interface CustomerSheetSavedSelectionDataSource {

    suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?>
}
