package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.Async

internal data class ConsentState(
    val consent: Async<Payload> = Async.Uninitialized,
    val merchantLogos: List<String> = emptyList(),
    val acceptConsent: Async<FinancialConnectionsSessionManifest> = Async.Uninitialized,
    val viewEffect: ViewEffect? = null
) {

    data class Payload(
        val consent: ConsentPane,
        val merchantLogos: List<String>,
        val shouldShowMerchantLogos: Boolean,
        val genericScreen: Screen? = null,
    )

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()
    }
}

internal enum class ConsentClickableText(val value: String) {
    DATA("stripe://data-access-notice"),
    LEGAL_DETAILS("stripe://legal-details-notice"),
    MANUAL_ENTRY("stripe://manual-entry"),
}
