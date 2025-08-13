package com.stripe.android.ui.core.elements

import androidx.lifecycle.SavedStateHandle

class AutoCardScanData(
    private val hasSeenAutoCardScanInitialValue: Boolean,
    private val openCardScanAutomaticallyConfig: Boolean,
    private val savedStateHandle: SavedStateHandle
) {
    var shouldOpenCardScanAutomatically: Boolean
        get() = !hasSeenAutoCardScan && openCardScanAutomaticallyConfig
        private set(value) {}

    var hasSeenAutoCardScan: Boolean
        get() = savedStateHandle[KEY_HAS_SEEN_AUTO_CARD_SCAN] ?: hasSeenAutoCardScanInitialValue
        set(value) {
            savedStateHandle[KEY_HAS_SEEN_AUTO_CARD_SCAN] = value
        }

    init {
        hasSeenAutoCardScan = hasSeenAutoCardScanInitialValue
    }

    companion object {
        const val KEY_HAS_SEEN_AUTO_CARD_SCAN = "KEY_HAS_SEEN_AUTO_CARD_SCAN"
    }
}
