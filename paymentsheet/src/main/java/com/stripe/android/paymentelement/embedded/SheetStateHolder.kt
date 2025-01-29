package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SheetStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var sheetIsOpen: Boolean
        get() = savedStateHandle.get<Boolean>(SHEET_IS_OPEN_KEY) == true
        set(value) = savedStateHandle.set(SHEET_IS_OPEN_KEY, value)

    companion object {
        private const val SHEET_IS_OPEN_KEY = "SheetStateHolder_SHEET_IS_OPEN_KEY"
    }
}
