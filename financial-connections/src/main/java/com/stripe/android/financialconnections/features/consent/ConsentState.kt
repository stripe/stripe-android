package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.ConsentPane

internal data class ConsentState(
    val manualEntryEnabled: Boolean = false,
    val manualEntryShowBusinessDaysNotice: Boolean = false,
    val consent: Async<ConsentPane> = Uninitialized,
    val acceptConsent: Async<Unit> = Uninitialized,
    val viewEffect: ViewEffect? = null
) : MavericksState {

    sealed interface ViewEffect {
        data class OpenUrl(val url: String) : ViewEffect
        object OpenBottomSheet : ViewEffect
    }
}

internal enum class ConsentClickableText(val value: String) {
    TERMS("terms"),
    PRIVACY("privacy"),
    DISCONNECT("disconnect"),
    DATA("data"),
    PRIVACY_CENTER("privacy_center"),
    DATA_ACCESS("data_access"),
    MANUAL_ENTRY("manual_entry")
}
