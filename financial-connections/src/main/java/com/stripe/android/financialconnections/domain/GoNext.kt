package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager
) {

    operator fun invoke(manifest: FinancialConnectionsSessionManifest) {
        navigationManager.navigate(
            manifest.nextPane.toNavigationCommand()
        )
    }

    @Suppress("ComplexMethod")
    private fun NextPane.toNavigationCommand() = when (this) {
        NextPane.ACCOUNT_PICKER -> TODO()
        NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT -> TODO()
        NextPane.AUTH_OPTIONS -> TODO()
        NextPane.CONSENT -> NavigationDirections.consent
        NextPane.INSTITUTION_PICKER -> NavigationDirections.bankPicker
        NextPane.LINK_CONSENT -> TODO()
        NextPane.LINK_LOGIN -> TODO()
        NextPane.MANUAL_ENTRY -> TODO()
        NextPane.MANUAL_ENTRY_SUCCESS -> TODO()
        NextPane.NETWORKING_LINK_LOGIN_WARMUP -> TODO()
        NextPane.NETWORKING_LINK_SIGNUP_PANE -> TODO()
        NextPane.NETWORKING_LINK_VERIFICATION -> TODO()
        NextPane.PARTNER_AUTH -> TODO()
        NextPane.SUCCESS -> TODO()
        NextPane.UNEXPECTED_ERROR -> TODO()
    }
}
