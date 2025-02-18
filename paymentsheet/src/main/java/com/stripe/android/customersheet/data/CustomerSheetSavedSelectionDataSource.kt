package com.stripe.android.customersheet.data

import com.stripe.android.paymentsheet.model.SavedSelection

/**
 * [CustomerSheetSavedSelectionDataSource] defines a set of operations for managing saved payment selections within a
 * [com.stripe.android.customersheet.CustomerSheet] context.
 */
internal interface CustomerSheetSavedSelectionDataSource {
    /**
     * Retrieves a saved selection
     *
     * @return a result containing the saved selection if operation was successful
     */
    suspend fun retrieveSavedSelection(
        customerSessionElementsSession: CustomerSessionElementsSession?
    ): CustomerSheetDataResult<SavedSelection?>

    suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit>
}
