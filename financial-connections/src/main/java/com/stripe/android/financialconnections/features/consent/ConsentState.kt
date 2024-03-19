package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.core.Result
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal data class ConsentState(
    val consent: Result<Payload> = Result.Uninitialized,
    val merchantLogos: List<String> = emptyList(),
    val currentBottomSheet: BottomSheetContent = BottomSheetContent.DATA,
    val acceptConsent: Result<FinancialConnectionsSessionManifest> = Result.Uninitialized,
    val viewEffect: ViewEffect? = null
) {

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
