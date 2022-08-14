package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

/**
 * Usecase that determines the next step to navigate based on the received input.
 *
 * @see [FinancialConnectionsSessionManifest.nextPane]
 * @see [FinancialConnectionsAuthorizationSession.nextPane]
 * @see [com.stripe.android.financialconnections.model.PartnerAccountsList.nextPane]
 *
 */
internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    private val getManifest: GetManifest,
    private val logger: Logger
) {

    suspend operator fun invoke(currentPane: NavigationCommand): NavigationCommand {
        val manifest = getManifest()
        val nextPane = when (currentPane.destination) {
            /**
             * institution picker step receives a fresh [FinancialConnectionsAuthorizationSession]
             * after picking a bank, source of truth for navigation.
             */
            NavigationDirections.institutionPicker.destination ->
                manifest.activeAuthSession!!.nextPane.toNavigationCommand()
            /**
             * Consent step receives a fresh [FinancialConnectionsSessionManifest]
             * after agreeing, source of truth for navigation.
             */
            NavigationDirections.consent.destination ->
                manifest.nextPane.toNavigationCommand()
            /**
             * Auth session authorization endpoint receives a
             * fresh [FinancialConnectionsAuthorizationSession], source of truth for navigation.
             */
            NavigationDirections.partnerAuth.destination ->
                manifest.activeAuthSession!!.nextPane.toNavigationCommand()
            /**
             * Account selection returns a [PartnerAccountsList] that includes the next pane,
             * source of truth for navigation.
             */
            NavigationDirections.accountPicker.destination -> {

                getAuthorizationSessionAccounts(manifest.activeAuthSession!!.id).nextPane.toNavigationCommand()
            }
            else -> TODO()
        }
        logger.debug("Navigating to next pane: ${nextPane.destination}")
        navigationManager.navigate(nextPane)
        return nextPane
    }

    @Suppress("ComplexMethod")
    private fun NextPane.toNavigationCommand(): NavigationCommand = when (this) {
        NextPane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
        NextPane.PARTNER_AUTH -> NavigationDirections.partnerAuth
        NextPane.CONSENT -> NavigationDirections.consent
        NextPane.ACCOUNT_PICKER -> NavigationDirections.accountPicker
        NextPane.SUCCESS -> NavigationDirections.success
        NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT,
        NextPane.AUTH_OPTIONS,
        NextPane.LINK_CONSENT,
        NextPane.LINK_LOGIN,
        NextPane.MANUAL_ENTRY,
        NextPane.MANUAL_ENTRY_SUCCESS,
        NextPane.NETWORKING_LINK_LOGIN_WARMUP,
        NextPane.NETWORKING_LINK_SIGNUP_PANE,
        NextPane.NETWORKING_LINK_VERIFICATION,
        NextPane.UNEXPECTED_ERROR -> {
            TODO("Unimplemented navigation command: ${this.value}")
        }
    }
}
