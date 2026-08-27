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
        set(value) = savedStateHandle.set(SHEET_IS_OPEN_KEY, value)

    var declinedLink2FA: Boolean
        get() = savedStateHandle.get<Boolean>(DECLINED_LINK_2FA_KEY) == true
        set(value) = savedStateHandle.set(DECLINED_LINK_2FA_KEY, value)

    companion object {
        private const val SHEET_IS_OPEN_KEY = "SheetStateHolder_SHEET_IS_OPEN_KEY"
        private const val DECLINED_LINK_2FA_KEY = "SheetStateHolder_DECLINED_LINK_2FA_KEY"
    }
}
