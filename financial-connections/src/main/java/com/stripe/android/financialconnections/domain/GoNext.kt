package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager,
    private val logger: Logger
) {

    operator fun invoke(
        currentPane: NavigationCommand,
        manifest: FinancialConnectionsSessionManifest,
        authorizationSession: FinancialConnectionsAuthorizationSession?
    ): NavigationCommand {
        val nextPane = when (currentPane.destination) {
            /**
             * institution picker step receives a fresh [FinancialConnectionsAuthorizationSession]
             * after picking a bank, source of truth for navigation.
             */
            NavigationDirections.institutionPicker.destination ->
                authorizationSession!!.nextPane.toNavigationCommand()
            /**
             * Consent step receives a fresh [FinancialConnectionsSessionManifest]
             * after agreeing, source of truth for navigation.
             */
            NavigationDirections.consent.destination ->
                manifest.nextPane.toNavigationCommand()
            else -> TODO()
        }
        logger.debug("Navigating to next pane: ${nextPane.destination}")
        navigationManager.navigate(nextPane)
        return nextPane
    }

    @Suppress("ComplexMethod")
    private fun NextPane.toNavigationCommand() = when (this) {
        NextPane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
        NextPane.PARTNER_AUTH -> NavigationDirections.partnerAuth
        NextPane.ACCOUNT_PICKER -> TODO()
        NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT -> TODO()
        NextPane.AUTH_OPTIONS -> TODO()
        NextPane.CONSENT -> NavigationDirections.consent
        NextPane.LINK_CONSENT -> TODO()
        NextPane.LINK_LOGIN -> TODO()
        NextPane.MANUAL_ENTRY -> TODO()
        NextPane.MANUAL_ENTRY_SUCCESS -> TODO()
        NextPane.NETWORKING_LINK_LOGIN_WARMUP -> TODO()
        NextPane.NETWORKING_LINK_SIGNUP_PANE -> TODO()
        NextPane.NETWORKING_LINK_VERIFICATION -> TODO()
        NextPane.SUCCESS -> TODO()
        NextPane.UNEXPECTED_ERROR -> TODO()
    }
}
