package com.stripe.android.financialconnections.features.consent

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.ConsentPane

internal data class ConsentState(
    val consent: Async<Payload> = Uninitialized,
    val merchantLogos: List<String> = emptyList(),
    val currentBottomSheet: BottomSheetContent = BottomSheetContent.DATA,
    val acceptConsent: Async<Unit> = Uninitialized,
    val viewEffect: ViewEffect? = null
) : MavericksState {

    data class Payload(
        val consent: ConsentPane,
        val merchantLogos: List<String>,
        val shouldShowMerchantLogos: Boolean
    )

    enum class BottomSheetContent {
        LEGAL,
        DATA
    }

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()

        data class OpenBottomSheet(
            val id: Long
        ) : ViewEffect()
    }
}

internal enum class ConsentClickableText(val value: String) {
    DATA("stripe://data-access-notice"),
    LEGAL_DETAILS("stripe://legal-details-notice"),
    MANUAL_ENTRY("stripe://manual-entry"),
}
