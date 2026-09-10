package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SheetStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var sheetLauncher: EmbeddedSheetLauncher? = null

    var sheetIsOpen: Boolean
        get() = savedStateHandle.get<Boolean>(SHEET_IS_OPEN_KEY) == true
        set(value) {
            savedStateHandle[SHEET_IS_OPEN_KEY] = value
            savedStateHandle[SHEET_STATE_VERSION_KEY] = sheetStateVersion + 1
        }

    val sheetStateVersion: Int
        get() = savedStateHandle.get<Int>(SHEET_STATE_VERSION_KEY) ?: 0

    companion object {
        private const val SHEET_IS_OPEN_KEY = "SheetStateHolder_SHEET_IS_OPEN_KEY"
        private const val SHEET_STATE_VERSION_KEY = "SheetStateHolder_SHEET_STATE_VERSION_KEY"
    }
}
