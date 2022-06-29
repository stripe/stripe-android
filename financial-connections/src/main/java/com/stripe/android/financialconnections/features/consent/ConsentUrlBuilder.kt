package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.AccountDisconnectionMethod
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls

/**
 * Helper to derive urls on the consent screen based on the manifest content.
 */
internal object ConsentUrlBuilder {
    fun getDisconnectUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.accountDisconnectionMethod) {
            AccountDisconnectionMethod.DASHBOARD -> FinancialConnectionsUrls.Disconnect.dashboard
            AccountDisconnectionMethod.SUPPORT -> FinancialConnectionsUrls.Disconnect.support
            AccountDisconnectionMethod.EMAIL, null -> FinancialConnectionsUrls.Disconnect.email
        }
    }

    fun getFAQUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.isStripeDirect ?: false) {
            true -> FinancialConnectionsUrls.FAQ.stripe
            false -> FinancialConnectionsUrls.FAQ.merchant
        }
    }

    fun getDataPolicyUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.isStripeDirect ?: false) {
            true -> FinancialConnectionsUrls.DataPolicy.stripe
            false -> FinancialConnectionsUrls.DataPolicy.merchant
        }
    }

    fun getStripeTOSUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.isEndUserFacing ?: false) {
            true -> FinancialConnectionsUrls.StripeToS.endUser
            false -> FinancialConnectionsUrls.StripeToS.merchantUser
        }
    }

    fun getPrivacyCenterUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.isStripeDirect ?: false) {
            true -> FinancialConnectionsUrls.PrivacyCenter.stripe
            false -> FinancialConnectionsUrls.PrivacyCenter.merchant
        }
    }
}
