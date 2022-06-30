package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
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
    ): TextResource = when (val name = manifest.name) {
        null -> TextResource.StringId(
            R.string.stripe_consent_requested_data_title_no_businessname
        )
        else -> TextResource.StringId(
            R.string.stripe_consent_requested_data_title,
            listOf(name)
        )
    }

    fun getConsentTitle(manifest: FinancialConnectionsSessionManifest): TextResource {
        val name = manifest.name
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
        val name = manifest.name
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
                FinancialConnectionsAccount.Permissions.BALANCES ->
                    Pair(
                        TextResource.StringId(R.string.stripe_consent_requested_data_balances_title),
                        TextResource.StringId(R.string.stripe_consent_requested_data_balances_desc)
                    )
                FinancialConnectionsAccount.Permissions.OWNERSHIP ->
                    Pair(
                        TextResource.StringId(R.string.stripe_consent_requested_data_ownership_title),
                        TextResource.StringId(R.string.stripe_consent_requested_data_ownership_desc)
                    )
                FinancialConnectionsAccount.Permissions.PAYMENT_METHOD ->
                    Pair(
                        TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_title),
                        TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_desc)
                    )
                FinancialConnectionsAccount.Permissions.TRANSACTIONS ->
                    Pair(
                        TextResource.StringId(R.string.stripe_consent_requested_data_transactions_title),
                        TextResource.StringId(R.string.stripe_consent_requested_data_transactions_desc)
                    )
                FinancialConnectionsAccount.Permissions.UNKNOWN -> null
            }
        }
    }

    private val FinancialConnectionsSessionManifest.name
        get() = businessName ?: connectPlatformName
}
