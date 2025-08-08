package com.stripe.android.ui.core.elements

import androidx.lifecycle.SavedStateHandle

class DirectToCardScanData(
    shouldOpenCardScanAutomaticallyInitialValue: Boolean,
    private val savedStateHandle: SavedStateHandle
) {
    var shouldOpenCardScanAutomatically: Boolean
        get() = savedStateHandle[KEY_SHOULD_OPEN_CARD_SCAN] ?: false
        set(value) = savedStateHandle.set(KEY_SHOULD_OPEN_CARD_SCAN, value)
    init {
        shouldOpenCardScanAutomatically = shouldOpenCardScanAutomaticallyInitialValue
    }

    companion object {
        const val KEY_SHOULD_OPEN_CARD_SCAN = "KEY_SHOULD_OPEN_CARD_SCAN"
    }
}
