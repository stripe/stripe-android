package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

/**
 * Gets cached partner accounts. If they're not found, they'll be fetched from backend (this
 * can happen on process kills).
 */
internal class GetAuthorizationSessionAccounts @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        authSessionId: String
    ): PartnerAccountsList {
        return repository.getOrFetchAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            sessionId = authSessionId
        )
    }
}
