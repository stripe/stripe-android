package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

internal class GoNext @Inject constructor(
    private val navigationManager: NavigationManager
) {

    operator fun invoke() {
    }

    operator fun invoke(manifest: FinancialConnectionsSessionManifest) {
    }
}