package com.stripe.android.financialconnections.features.partnerauth

import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized

internal data class SharedPartnerAuthState(
    val pane: Pane,
    val payload: Async<Payload> = Uninitialized,
    val viewEffect: ViewEffect? = null,
    val authenticationStatus: Async<AuthenticationStatus> = Uninitialized,
    val inModal: Boolean = false,
) {

    val isRelinkSession: Boolean
        get() = pane == Pane.BANK_AUTH_REPAIR

    constructor(args: PartnerAuthViewModel.Args) : this(
        pane = args.pane,
        inModal = args.inModal,
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
                payload !is Fail &&
                !isRelinkSession

    sealed interface ViewEffect {
        data class OpenPartnerAuth(
            val url: String
        ) : ViewEffect

        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect
    }

    internal enum class ClickableText(val value: String) {
        DATA("stripe://data-access-notice"),
    }
}
