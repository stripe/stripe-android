package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchFinancialConnectionsSession @Inject constructor(
    private val fetchPaginatedAccountsForSession: FetchPaginatedAccountsForSession,
    private val financialConnectionsRepository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration

) {

    /**
     * Fetches the session with all the connected accounts via pagination
     *
     * @return [FinancialConnectionsSession] with all connected accounts
     */
    suspend operator fun invoke(): FinancialConnectionsSession {
        val session = financialConnectionsRepository.getFinancialConnectionsSession(
            configuration.financialConnectionsSessionClientSecret
        )
        return fetchPaginatedAccountsForSession(session)
    }
}
