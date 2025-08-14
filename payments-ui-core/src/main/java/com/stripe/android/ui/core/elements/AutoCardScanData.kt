package com.stripe.android.ui.core.elements

import androidx.lifecycle.SavedStateHandle

class AutoCardScanData(
    private val hasSeenAutoCardScanOpenInitialValue: Boolean,
    private val openCardScanAutomaticallyConfig: Boolean,
    private val savedStateHandle: SavedStateHandle
) {
    var shouldOpenCardScanAutomatically: Boolean
        get() = !hasSeenAutoCardScanOpen && openCardScanAutomaticallyConfig
        private set(value) {}

    var hasSeenAutoCardScanOpen: Boolean
        get() = savedStateHandle[KEY_HAS_SEEN_AUTO_CARD_SCAN_OPEN] ?: hasSeenAutoCardScanOpenInitialValue
        set(value) {
            savedStateHandle[KEY_HAS_SEEN_AUTO_CARD_SCAN_OPEN] = value
        }

    init {
        hasSeenAutoCardScanOpen = hasSeenAutoCardScanOpenInitialValue
    }

    companion object {
        const val KEY_HAS_SEEN_AUTO_CARD_SCAN_OPEN = "KEY_HAS_SEEN_AUTO_CARD_SCAN_OPEN"
    }
}
