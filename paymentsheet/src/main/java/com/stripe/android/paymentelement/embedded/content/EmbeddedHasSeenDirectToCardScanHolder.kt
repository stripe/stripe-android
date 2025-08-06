package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedHasSeenDirectToCardScanHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var hasSeenDirectToCardScan: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_SEEN_DIRECT_TO_CARD_SCAN_KEY) == true
        set(value) = savedStateHandle.set(HAS_SEEN_DIRECT_TO_CARD_SCAN_KEY, value)

    companion object {
        private const val HAS_SEEN_DIRECT_TO_CARD_SCAN_KEY = "HAS_SEEN_DIRECT_TO_CARD_SCAN_KEY"
    }
}
