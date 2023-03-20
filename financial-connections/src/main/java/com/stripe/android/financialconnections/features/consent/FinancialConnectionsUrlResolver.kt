package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.AccountDisconnectionMethod
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls

/**
 * Helper to derive urls based on the manifest content.
 */
internal object FinancialConnectionsUrlResolver {
    fun getDisconnectUrl(manifest: FinancialConnectionsSessionManifest): String {
        return when (manifest.accountDisconnectionMethod ?: AccountDisconnectionMethod.UNKNOWN) {
            AccountDisconnectionMethod.SUPPORT -> when (manifest.isEndUserFacing ?: false) {
                true -> FinancialConnectionsUrls.Disconnect.supportEndUser
                false -> FinancialConnectionsUrls.Disconnect.supportMerchantUser
            }
            AccountDisconnectionMethod.DASHBOARD -> FinancialConnectionsUrls.Disconnect.dashboard
            AccountDisconnectionMethod.LINK -> FinancialConnectionsUrls.Disconnect.link
            AccountDisconnectionMethod.EMAIL,
            AccountDisconnectionMethod.UNKNOWN -> FinancialConnectionsUrls.Disconnect.email
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

    fun getPartnerNotice(isStripeDirect: Boolean): String {
        return when (isStripeDirect) {
            true -> FinancialConnectionsUrls.PartnerNotice.stripe
            false -> FinancialConnectionsUrls.PartnerNotice.merchant
        }
    }

    const val supportUrl: String = "https://support.stripe.com/contact"
    const val linkVerificationSupportUrl: String = "https://support.link.co/contact/email?skipVerification=true"
}
