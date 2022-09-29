package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.model.Token
import javax.inject.Inject

internal class FetchFinancialConnectionsSessionForToken @Inject constructor(
    private val connectionsRepository: FinancialConnectionsRepository
) {

    /**
     * Fetches the [FinancialConnectionsSession] and its nested [Token].
     *
     * @param clientSecret the [FinancialConnectionsSession] client secret
     *
     * @return pair of [FinancialConnectionsSession] and [Token]
     */
    suspend operator fun invoke(clientSecret: String): Pair<FinancialConnectionsSession, Token> {
        val session = connectionsRepository.getFinancialConnectionsSession(clientSecret)
        val parsedToken = session.parsedToken
        requireNotNull(parsedToken) { "Could not extract Token from FinancialConnectionsSession." }
        return session to parsedToken
    }
}
