package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedHasAutomaticallyLaunchedCardScanHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var hasAutomaticallyLaunchedCardScan: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN_KEY) ?: false
        set(value) = savedStateHandle.set(HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN_KEY, value)

    var isLaunchingCardFormWithCardScanEnabled: Boolean
        get() = savedStateHandle[IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY] ?: false
        set(value) = savedStateHandle.set(IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY, value)

    companion object {
        private const val HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN_KEY = "HAS_AUTOMATICALLY_LAUNCHED_CARD_SCAN_KEY"
        private const val IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY =
            "IS_LAUNCHING_CARD_FORM_WITH_CARD_SCAN_ENABLED_KEY"
    }
}
