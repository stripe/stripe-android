package com.stripe.android.connections.domain

import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.repository.ConnectionsRepository
import com.stripe.android.model.Token
import com.stripe.android.model.parsers.TokenJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class FetchLinkAccountSessionForToken @Inject constructor(
    private val connectionsRepository: ConnectionsRepository
) {

    /**
     * Fetches the [LinkAccountSession] and its nested [Token].
     *
     * @param clientSecret the link account session client secret
     *
     * @return pair of [LinkAccountSession] and [Token]
     */
    suspend operator fun invoke(clientSecret: String): Pair<LinkAccountSession, Token> {
        val linkAccountSession = connectionsRepository.getLinkAccountSession(clientSecret)
        val parsedToken = linkAccountSession.token?.let { TokenJsonParser().parse(JSONObject(it)) }
        requireNotNull(parsedToken) { "Could not extract Token from LinkedAccountSession." }
        return linkAccountSession to parsedToken
    }
}
