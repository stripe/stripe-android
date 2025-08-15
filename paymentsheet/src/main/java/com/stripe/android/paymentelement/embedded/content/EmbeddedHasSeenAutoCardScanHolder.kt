package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedHasSeenAutoCardScanHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var hasSeenAutoCardScanOpen: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY) ?: false
        set(value) = savedStateHandle.set(HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY, value)

    var isLaunchingCardFormWithCardScanEnabled: Boolean
        get() = savedStateHandle[IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY] ?: false
        set(value) = savedStateHandle.set(IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY, value)

    companion object {
        private const val HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY = "HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY"
        private const val IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY =
            "IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY"
    }
}
