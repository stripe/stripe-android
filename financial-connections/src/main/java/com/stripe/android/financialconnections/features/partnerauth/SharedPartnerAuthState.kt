package com.stripe.android.financialconnections.features.partnerauth

import android.os.Bundle
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

internal data class SharedPartnerAuthState(
    /**
     * The active auth session id. Used across process kills to prevent re-creating the session
     * if one is already active.
     */
    val activeAuthSession: String?,
    val pane: Pane,
    val payload: Async<Payload>,
    val viewEffect: ViewEffect?,
    val authenticationStatus: Async<AuthenticationStatus>,
) {

    constructor(pane: Pane, savedState: Bundle?) : this(
        activeAuthSession = savedState?.getString(KEY_ACTIVE_AUTH_SESSION),
        pane = pane,
        payload = Uninitialized,
        viewEffect = null,
        authenticationStatus = Uninitialized
    )

    data class Payload(
        val isStripeDirect: Boolean,
        val institution: FinancialConnectionsInstitution,
        val authSession: FinancialConnectionsAuthorizationSession,
    )

    data class AuthenticationStatus(
        val action: Action,
    ) {
        enum class Action {
            CANCELLING,
            AUTHENTICATING
        }
    }

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

    companion object {
        const val KEY_SAVED_STATE = "SharedPartnerAuthState"
        const val KEY_ACTIVE_AUTH_SESSION = "activeAuthSession"
    }
}
