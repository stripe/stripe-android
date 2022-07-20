package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.ui.TextResource

internal data class ConsentState(
    val title: TextResource = TextResource.Text(""),
    val disconnectUrl: String = "",
    val stripeToSUrl: String = "",
    val faqUrl: String = "",
    val dataPolicyUrl: String = "",
    val privacyCenterUrl: String = "",
    val bullets: List<Pair<Int, TextResource>> = emptyList(),
    val requestedDataTitle: TextResource = TextResource.Text(""),
    val requestedDataBullets: List<Pair<TextResource, TextResource>> = emptyList(),
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
    DATA_ACCESS("data_access")
}
