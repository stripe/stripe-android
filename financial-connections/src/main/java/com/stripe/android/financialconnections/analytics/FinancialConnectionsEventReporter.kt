package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.navigation.NavigationCommand

/**
 * Contract for connections related analytic events that will be tracked.
 *
 * Implementation will encapsulate the firing event logic.
 *
 * @see [FinancialConnectionsAnalyticsEvent].
 *
 */
internal interface FinancialConnectionsEventReporter {

    fun onPresented(configuration: FinancialConnectionsSheet.Configuration)

    fun onPaneLaunched(
        current: NavigationCommand,
        next: NavigationCommand
    )

    fun onResult(
        configuration: FinancialConnectionsSheet.Configuration,
        financialConnectionsSheetResult: FinancialConnectionsSheetActivityResult
    )

    fun onPaneLoaded(pane: NavigationCommand)
    fun onClickSecurityStripe(pane: NavigationCommand)
    fun onClickDataRequested(pane: NavigationCommand)
    fun onClickLegalLearnMore(pane: NavigationCommand)
    fun onClickDataAccessLearnMore(pane: NavigationCommand)
    fun onClickLegalPrivacyPolicy(pane: NavigationCommand)
    fun onClickLegalTerms(pane: NavigationCommand)
    fun onClickDisconnect(pane: NavigationCommand)
    fun onClickAgree(pane: NavigationCommand)
}
