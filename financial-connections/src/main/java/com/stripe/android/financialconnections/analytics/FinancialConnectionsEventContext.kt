package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

internal class FinancialConnectionsEventContext(
    initialManifest: FinancialConnectionsSessionManifest?
) {
    @Volatile
    var manifest: FinancialConnectionsSessionManifest? = initialManifest
        private set

    /** The pane on screen, or null outside the native flow. */
    @Volatile
    var currentPane: Pane? = null
        private set

    fun update(manifest: FinancialConnectionsSessionManifest) {
        this.manifest = manifest
    }

    fun updateCurrentPane(pane: Pane) {
        currentPane = pane
    }
}
