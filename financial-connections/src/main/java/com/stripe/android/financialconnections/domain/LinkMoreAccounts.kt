package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

/**
 * Resets the current session allowing to link more accounts.
 *
 * Returns a refreshed manifest instance.
 */
internal class LinkMoreAccounts @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(): FinancialConnectionsSessionManifest {
        return repository.postMarkLinkingMoreAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret
        )
    }
}
