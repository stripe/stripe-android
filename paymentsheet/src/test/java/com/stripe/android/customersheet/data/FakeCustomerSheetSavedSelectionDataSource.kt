package com.stripe.android.customersheet.data

import com.stripe.android.paymentsheet.model.SavedSelection

private typealias SetSavedSelectionOperation = (selection: SavedSelection?) -> CustomerSheetDataResult<Unit>

internal class FakeCustomerSheetSavedSelectionDataSource(
    private var savedSelection: CustomerSheetDataResult<SavedSelection?> =
        CustomerSheetDataResult.success(null),
    private val onSetSavedSelection: SetSavedSelectionOperation? = null
) : CustomerSheetSavedSelectionDataSource {
    override suspend fun retrieveSavedSelection(
        customerSessionElementsSession: CustomerSessionElementsSession?
    ): CustomerSheetDataResult<SavedSelection?> {
        return savedSelection
    }

    override suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit> {
        return onSetSavedSelection?.invoke(selection) ?: run {
            savedSelection = CustomerSheetDataResult.success(selection)
            CustomerSheetDataResult.success(Unit)
        }
    }
}
