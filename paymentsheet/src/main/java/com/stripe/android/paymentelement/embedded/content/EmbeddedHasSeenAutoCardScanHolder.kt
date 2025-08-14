package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedHasSeenAutoCardScanHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var hasSeenAutoCardScanOpen: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY) == true
        set(value) = savedStateHandle.set(HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY, value)

    companion object {
        private const val HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY = "HAS_SEEN_AUTO_CARD_SCAN_OPEN_KEY"
    }
}
