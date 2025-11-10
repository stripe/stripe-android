package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakePrefsRepository(
    private val setSavedSelectionResult: Boolean = true,
) : PrefsRepository {
    private var savedSelection: SavedSelection = SavedSelection.None

    override suspend fun getSavedSelection(isGooglePayAvailable: Boolean, isLinkAvailable: Boolean): SavedSelection =
        savedSelection

    override fun setSavedSelection(savedSelection: SavedSelection?): Boolean {
        savedSelection?.let {
            this.savedSelection = it
        }
        return setSavedSelectionResult
    }
}
