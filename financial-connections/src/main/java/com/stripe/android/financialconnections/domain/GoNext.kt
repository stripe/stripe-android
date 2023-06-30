package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

/**
 * Navigates to the next screen given a [Pane].
 */
internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager,
    private val logger: Logger
) {

    operator fun invoke(
        nextPane: Pane,
        args: Map<String, Any?> = emptyMap()
    ): NavigationCommand {
        val nextPaneDirection = nextPane.toNavigationCommand(args)
        logger.debug("Navigating to next pane: ${nextPaneDirection.destination}")
        navigationManager.navigate(nextPaneDirection)
        return nextPaneDirection
    }
}

@Suppress("ComplexMethod")
internal fun Pane.toNavigationCommand(
    args: Map<String, Any?>
): NavigationCommand = when (this) {
    Pane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
    Pane.PARTNER_AUTH -> NavigationDirections.partnerAuth
    Pane.CONSENT -> NavigationDirections.consent
    Pane.ACCOUNT_PICKER -> NavigationDirections.accountPicker
    Pane.SUCCESS -> NavigationDirections.success
    Pane.MANUAL_ENTRY -> NavigationDirections.manualEntry
    Pane.MANUAL_ENTRY_SUCCESS -> NavigationDirections.ManualEntrySuccess(args)
    Pane.ATTACH_LINKED_PAYMENT_ACCOUNT -> NavigationDirections.attachLinkedPaymentAccount
    Pane.RESET -> NavigationDirections.reset
    Pane.NETWORKING_LINK_SIGNUP_PANE -> NavigationDirections.networkingLinkSignup
    Pane.NETWORKING_LINK_LOGIN_WARMUP -> NavigationDirections.networkingLinkLoginWarmup
    Pane.NETWORKING_LINK_VERIFICATION -> NavigationDirections.networkingLinkVerification
    Pane.LINK_STEP_UP_VERIFICATION -> NavigationDirections.linkStepUpVerification
    Pane.LINK_ACCOUNT_PICKER -> NavigationDirections.linkAccountPicker
    Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION -> NavigationDirections.networkingSaveToLinkVerification
    Pane.BANK_AUTH_REPAIR,
    Pane.AUTH_OPTIONS,
    Pane.LINK_CONSENT,
    Pane.LINK_LOGIN,
    Pane.UNEXPECTED_ERROR -> {
        TODO("Unimplemented navigation command: ${this.value}")
    }

}
