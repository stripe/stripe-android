package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

/**
 * Canonical session information for one presentation, shared by client and response events.
 * Reading this context never performs a request or derives an ID from a client secret.
 */
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
