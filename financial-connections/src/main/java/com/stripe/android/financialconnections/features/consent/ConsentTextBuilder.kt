package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

/**
 * Helper to derive text content on the consent screen based on the manifest content.
 */
internal object ConsentTextBuilder {

    fun getBusinessName(manifest: FinancialConnectionsSessionManifest): String? {
        return manifest.businessName ?: manifest.connectPlatformName
    }
}
