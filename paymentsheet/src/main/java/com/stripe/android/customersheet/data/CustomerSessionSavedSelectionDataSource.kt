package com.stripe.android.customersheet.data

import com.stripe.android.paymentsheet.model.SavedSelection
import javax.inject.Inject

internal class CustomerSessionSavedSelectionDataSource @Inject constructor() : CustomerSheetSavedSelectionDataSource {
    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        throw NotImplementedError("Not implemented yet!")
    }

    override suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit> {
        throw NotImplementedError("Not implemented yet!")
    }
}
