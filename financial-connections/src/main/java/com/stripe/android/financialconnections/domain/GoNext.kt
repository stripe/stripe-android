package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.ClientPane
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

/**
 * Navigates to the next screen given a [ClientPane].
 */
internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager,
    private val logger: Logger
) {

    operator fun invoke(
        nextPane: ClientPane,
        args: Map<String, Any?> = emptyMap()
    ): NavigationCommand {
        val nextPaneDirection = nextPane.toNavigationCommand(logger, args)
        logger.debug("Navigating to next pane: ${nextPaneDirection.destination}")
        navigationManager.navigate(nextPaneDirection)
        return nextPaneDirection
    }
}

@Suppress("ComplexMethod")
internal fun ClientPane.toNavigationCommand(
    logger: Logger,
    args: Map<String, Any?>
): NavigationCommand = when (this) {
    ClientPane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
    ClientPane.PARTNER_AUTH -> NavigationDirections.partnerAuth
    ClientPane.CONSENT -> NavigationDirections.consent
    ClientPane.ACCOUNT_PICKER -> NavigationDirections.accountPicker
    ClientPane.SUCCESS -> NavigationDirections.success
    ClientPane.MANUAL_ENTRY -> NavigationDirections.manualEntry
    ClientPane.MANUAL_ENTRY_SUCCESS -> NavigationDirections.ManualEntrySuccess(args)
    ClientPane.ATTACH_LINKED_PAYMENT_ACCOUNT -> NavigationDirections.attachLinkedPaymentAccount
    ClientPane.RESET -> NavigationDirections.reset
    ClientPane.NETWORKING_LINK_SIGNUP_PANE -> {
        logger.error("Link not supported on native flows yet. Navigating to Success.")
        NavigationDirections.success
    }
    ClientPane.AUTH_OPTIONS,
    ClientPane.LINK_CONSENT,
    ClientPane.LINK_LOGIN,
    ClientPane.NETWORKING_LINK_LOGIN_WARMUP,
    ClientPane.NETWORKING_LINK_VERIFICATION,
    ClientPane.UNEXPECTED_ERROR -> {
        TODO("Unimplemented navigation command: ${this.value}")
    }
}
