package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.MavericksState
import com.stripe.android.financialconnections.ui.TextResource

internal data class ConsentState(
    val title: TextResource = TextResource.Text(""),
    val bullets: List<Pair<Int, TextResource>> = emptyList(),
    val requestedDataTitle: TextResource = TextResource.Text(""),
    val requestedDataBullets: List<Pair<TextResource, TextResource>> = emptyList(),
    val bottomSheetType: BottomSheetType = BottomSheetType.NONE,
    val viewEffect: ViewEffect? = null
) : MavericksState {

    enum class BottomSheetType {
        NONE, DATA
    }

    sealed interface ViewEffect {
        data class OpenUrl(val url: String) : ViewEffect
    }
}

internal enum class ConsentClickableText(val value: String) {
    TERMS("terms"),
    PRIVACY("privacy"),
    DISCONNECT("disconnect"),
    DATA("data"),
    MORE("more"),
    DATA_ACCESS("data_access")
}