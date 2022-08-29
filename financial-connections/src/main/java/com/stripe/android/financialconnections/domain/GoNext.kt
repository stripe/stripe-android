package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

/**
 * Navigates to the next screen given a [NextPane].
 */
internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager,
    private val logger: Logger
) {

    operator fun invoke(
        nextPane: NextPane,
        args: Map<String, Any?> = emptyMap()
    ): NavigationCommand {
        val nextPaneDirection = nextPane.toNavigationCommand(args)
        logger.debug("Navigating to next pane: ${nextPaneDirection.destination}")
        navigationManager.navigate(nextPaneDirection)
        return nextPaneDirection
    }

    @Suppress("ComplexMethod")
    private fun NextPane.toNavigationCommand(
        args: Map<String, Any?>
    ): NavigationCommand = when (this) {
        NextPane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
        NextPane.PARTNER_AUTH -> NavigationDirections.partnerAuth
        NextPane.CONSENT -> NavigationDirections.consent
        NextPane.ACCOUNT_PICKER -> NavigationDirections.accountPicker
        NextPane.SUCCESS -> NavigationDirections.success
        NextPane.MANUAL_ENTRY -> NavigationDirections.manualEntry
        NextPane.MANUAL_ENTRY_SUCCESS -> NavigationDirections.ManualEntrySuccess(args)
        NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT,
        NextPane.AUTH_OPTIONS,
        NextPane.LINK_CONSENT,
        NextPane.LINK_LOGIN,
        NextPane.NETWORKING_LINK_LOGIN_WARMUP,
        NextPane.NETWORKING_LINK_SIGNUP_PANE,
        NextPane.NETWORKING_LINK_VERIFICATION,
        NextPane.UNEXPECTED_ERROR -> {
            TODO("Unimplemented navigation command: ${this.value}")
        }
    }
}
