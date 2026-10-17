package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutomaticallyLaunchedCardScanFormDataHelper(
    private val hasAutomaticallyLaunchedCardScanInitialValue: Boolean,
    private val openCardScanAutomaticallyConfig: Boolean,
    private val savedStateHandle: SavedStateHandle
) {
    var shouldLaunchCardScanAutomatically: Boolean
        get() = !hasAutomaticallyLaunchedCardScan &&
            openCardScanAutomaticallyConfig
        private set(value) {}

    var hasAutomaticallyLaunchedCardScan: Boolean
        get() = savedStateHandle[KEY_HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN]
            ?: hasAutomaticallyLaunchedCardScanInitialValue
        set(value) {
            savedStateHandle[KEY_HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN] = value
        }

    init {
        hasAutomaticallyLaunchedCardScan = hasAutomaticallyLaunchedCardScanInitialValue
    }

    private companion object {
        const val KEY_HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN = "KEY_HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN"
    }
}
