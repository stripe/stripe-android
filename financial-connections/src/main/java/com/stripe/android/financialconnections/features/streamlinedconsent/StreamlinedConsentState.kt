package com.stripe.android.financialconnections.features.streamlinedconsent

import com.stripe.android.financialconnections.features.generic.GenericScreenState
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.StreamlinedConsentPane
import com.stripe.android.financialconnections.presentation.Async

internal data class StreamlinedConsentState(
    val payload: Async<Payload> = Async.Uninitialized,
    val acceptConsent: Async<FinancialConnectionsSessionManifest> = Async.Uninitialized,
    val viewEffect: ViewEffect? = null,
) {

    data class Payload(
        val streamlinedConsentPane: StreamlinedConsentPane,
        val dataAccessNotice: DataAccessNotice?,
    ) {

        val genericScreenState: GenericScreenState
            get() = GenericScreenState(
                screen = streamlinedConsentPane.screen,
                inModal = false,
                // The header icon is always the selected institution's logo.
                isHeaderIconInstitutionLogo = true,
            )
    }

    sealed interface ViewEffect {
        data class OpenUrl(val url: String) : ViewEffect
    }
}
