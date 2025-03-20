package com.stripe.android.financialconnections.features.streamlinedconsent

import com.stripe.android.financialconnections.features.generic.GenericScreenState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.IDConsentContentPane
import com.stripe.android.financialconnections.presentation.Async

internal data class IDConsentContentState(
    val payload: Async<Payload> = Async.Uninitialized,
    val acceptConsent: Async<FinancialConnectionsSessionManifest> = Async.Uninitialized,
    val viewEffect: ViewEffect? = null,
) {

    data class Payload(
        val idConsentContentPane: IDConsentContentPane,
    ) {

        val genericScreenState: GenericScreenState
            get() = GenericScreenState(idConsentContentPane.screen, inModal = false)
    }

    sealed interface ViewEffect {
        data class OpenUrl(val url: String) : ViewEffect
    }
}
