package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchFinancialConnectionsSession @Inject constructor(
    private val fetchPaginatedAccountsForSession: FetchPaginatedAccountsForSession,
    private val financialConnectionsRepository: FinancialConnectionsRepository
) {

    /**
     * Fetches the session with all the connected accounts via pagination
     *
     * @param clientSecret the [FinancialConnectionsSession] client secret
     *
     * @return [FinancialConnectionsSession] with all connected accounts
     */
    suspend operator fun invoke(clientSecret: String): FinancialConnectionsSession {
        val session = financialConnectionsRepository.getFinancialConnectionsSession(clientSecret)
        return fetchPaginatedAccountsForSession(session)
    }
}
