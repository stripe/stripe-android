package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal class FinancialConnectionsEventContext(
    initialManifest: FinancialConnectionsSessionManifest?
) {
    @Volatile
    var manifest: FinancialConnectionsSessionManifest? = initialManifest
        private set

    fun update(manifest: FinancialConnectionsSessionManifest) {
        this.manifest = manifest
    }
}
