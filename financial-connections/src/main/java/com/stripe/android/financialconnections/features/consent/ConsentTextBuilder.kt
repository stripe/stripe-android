package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Permissions
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.ui.TextResource

/**
 * Helper to derive text content on the consent screen based on the manifest content.
 */
internal object ConsentTextBuilder {

    fun getBullets(manifest: FinancialConnectionsSessionManifest): List<Pair<Int, TextResource>> {
        return listOf(
            R.drawable.stripe_ic_safe to getRequestedDataBullet(manifest),
            R.drawable.stripe_ic_shield to TextResource.StringId(R.string.stripe_consent_pane_body2),
            R.drawable.stripe_ic_lock to TextResource.StringId(R.string.stripe_consent_pane_body3)
        )
    }

    internal fun getDataRequestedTitle(
        manifest: FinancialConnectionsSessionManifest
    ): TextResource = when (val name = getBusinessName(manifest)) {
        null -> TextResource.StringId(
            R.string.stripe_consent_requested_data_title_no_businessname
        )
        else -> TextResource.StringId(
            R.string.stripe_consent_requested_data_title,
            listOf(name)
        )
    }

    fun getConsentTitle(manifest: FinancialConnectionsSessionManifest): TextResource {
        val name = getBusinessName(manifest)
        return when {
            name == null -> TextResource.StringId(
                R.string.stripe_consent_pane_title_no_businessname
            )
            manifest.connectedAccountName != null -> TextResource.StringId(
                R.string.stripe_consent_pane_title_connected_account,
                listOf(manifest.connectedAccountName, name)
            )
            else -> TextResource.StringId(
                R.string.stripe_consent_pane_title,
                listOf(name)
            )
        }
    }

    private fun getRequestedDataBullet(
        manifest: FinancialConnectionsSessionManifest
    ): TextResource {
        val name = getBusinessName(manifest)
        return when {
            name == null -> TextResource.StringId(
                R.string.stripe_consent_pane_body1_no_businessname
            )
            manifest.connectedAccountName != null -> TextResource.StringId(
                R.string.stripe_consent_pane_body1_connected_account,
                listOf(manifest.connectedAccountName, name)
            )
            else -> TextResource.StringId(
                R.string.stripe_consent_pane_body1,
                listOf(name)
            )
        }
    }

    fun getRequestedDataBullets(manifest: FinancialConnectionsSessionManifest): List<Pair<TextResource, TextResource>> {
        return manifest.permissions.mapNotNull { permission ->
            when (permission) {
                Permissions.BALANCES -> Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_balances_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_balances_desc)
                )
                Permissions.OWNERSHIP -> Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_ownership_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_ownership_desc)
                )
                Permissions.PAYMENT_METHOD,
                Permissions.ACCOUNT_NUMBERS -> Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_desc)
                )
                Permissions.TRANSACTIONS -> Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_transactions_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_transactions_desc)
                )
                Permissions.UNKNOWN -> null
            }
        }.distinct()
    }

    fun getBusinessName(manifest: FinancialConnectionsSessionManifest): String? {
        return manifest.businessName ?: manifest.connectPlatformName
    }
}
