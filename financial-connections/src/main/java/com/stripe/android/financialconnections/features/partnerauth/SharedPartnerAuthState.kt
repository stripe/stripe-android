package com.stripe.android.financialconnections.features.partnerauth

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal data class SharedPartnerAuthState(
    /**
     * The active auth session id. Used across process kills to prevent re-creating the session
     * if one is already active.
     */
    @PersistState
    val activeAuthSession: String? = null,
    val pane: FinancialConnectionsSessionManifest.Pane,
    val cancelling: Boolean = false,
    val payload: Async<Payload> = Uninitialized,
    val viewEffect: ViewEffect? = null,
    val authenticationStatus: Async<Action> = Uninitialized,
) : MavericksState {

    enum class Action {
        CANCELLING,
        AUTHENTICATING
    }

    data class Payload(
        val isStripeDirect: Boolean,
        val institution: FinancialConnectionsInstitution,
        val authSession: FinancialConnectionsAuthorizationSession,
    )

    val canNavigateBack: Boolean
        get() =
            // Authentication running -> don't allow back navigation
            authenticationStatus !is Loading &&
                authenticationStatus !is Success &&
                // Failures posting institution -> don't allow back navigation
                payload !is Fail

    sealed interface ViewEffect {
        data class OpenPartnerAuth(
            val url: String
        ) : ViewEffect

        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect

        data class OpenBottomSheet(
            val id: Long
        ) : ViewEffect
    }

    internal enum class ClickableText(val value: String) {
        DATA("stripe://data-access-notice"),
    }
}
