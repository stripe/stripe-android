package com.stripe.android.financialconnections.navigation

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

internal interface NavigationManager {
    val navigationState: MutableStateFlow<NavigationState>
    fun navigate(state: NavigationState)
    fun onNavigated(state: NavigationState)
}

internal class NavigationManagerImpl @Inject constructor(
    private val logger: Logger
) : NavigationManager {

    override val navigationState: MutableStateFlow<NavigationState> =
        MutableStateFlow(NavigationState.Idle)

    override fun navigate(state: NavigationState) {
        logger.debug("NavigationManager navigating to: $navigationState")
        navigationState.value = state
    }

    override fun onNavigated(state: NavigationState) {
        // clear navigation state, if state is the current state:
        navigationState.compareAndSet(state, NavigationState.Idle)
    }
}

@Suppress("ComplexMethod")
internal fun Pane.toNavigationCommand(
    args: Map<String, Any?> = emptyMap()
): NavigationCommand = when (this) {
    Pane.INSTITUTION_PICKER -> NavigationDirections.institutionPicker
    Pane.PARTNER_AUTH -> NavigationDirections.partnerAuth
    Pane.CONSENT -> NavigationDirections.consent
    Pane.ACCOUNT_PICKER -> NavigationDirections.accountPicker
    Pane.SUCCESS -> NavigationDirections.success
    Pane.MANUAL_ENTRY -> NavigationDirections.manualEntry
    Pane.MANUAL_ENTRY_SUCCESS ->
        NavigationDirections.ManualEntrySuccess(args)
    Pane.ATTACH_LINKED_PAYMENT_ACCOUNT -> NavigationDirections.attachLinkedPaymentAccount
    Pane.RESET -> NavigationDirections.reset
    Pane.NETWORKING_LINK_SIGNUP_PANE -> NavigationDirections.networkingLinkSignup
    Pane.NETWORKING_LINK_LOGIN_WARMUP -> NavigationDirections.networkingLinkLoginWarmup
    Pane.NETWORKING_LINK_VERIFICATION -> NavigationDirections.networkingLinkVerification
    Pane.LINK_STEP_UP_VERIFICATION -> NavigationDirections.linkStepUpVerification
    Pane.LINK_ACCOUNT_PICKER -> NavigationDirections.linkAccountPicker
    Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION -> NavigationDirections.networkingSaveToLinkVerification
    Pane.AUTH_OPTIONS,
    Pane.LINK_CONSENT,
    Pane.LINK_LOGIN,
    Pane.UNEXPECTED_ERROR -> {
        TODO("Unimplemented navigation command: ${this.value}")
    }
}
