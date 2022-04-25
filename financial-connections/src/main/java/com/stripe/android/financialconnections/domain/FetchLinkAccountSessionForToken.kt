package com.stripe.android.connections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.model.Token
import com.stripe.android.model.parsers.TokenJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class FetchLinkAccountSessionForToken @Inject constructor(
    private val connectionsRepository: FinancialConnectionsRepository
) {

    /**
     * Fetches the [FinancialConnectionsSession] and its nested [Token].
     *
     * @param clientSecret the link account session client secret
     *
     * @return pair of [FinancialConnectionsSession] and [Token]
     */
    suspend operator fun invoke(clientSecret: String): Pair<FinancialConnectionsSession, Token> {
        val linkAccountSession = connectionsRepository.getLinkAccountSession(clientSecret)
        val parsedToken =
            linkAccountSession.bankAccountToken?.let { TokenJsonParser().parse(JSONObject(it)) }
        requireNotNull(parsedToken) { "Could not extract Token from LinkedAccountSession." }
        return linkAccountSession to parsedToken
    }
}
