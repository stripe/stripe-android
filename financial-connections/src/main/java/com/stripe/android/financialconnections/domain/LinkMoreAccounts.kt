package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

/**
 * Gets cached partner accounts. If they're not found, they'll be fetched from backend (this
 * can happen on process kills).
 */
internal class LinkMoreAccounts @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(): FinancialConnectionsSessionManifest {
        return repository.postMarkLinkingMoreAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
        )
    }
}
