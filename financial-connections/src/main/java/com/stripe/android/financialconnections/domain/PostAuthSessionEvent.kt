package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import java.util.Date
import javax.inject.Inject

internal class PostAuthSessionEvent @Inject constructor(
    private val repository: FinancialConnectionsManifestRepository,
    private val logger: Logger,
    private val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(
        sessionId: String,
        events: List<AuthSessionEvent>
    ): Result<FinancialConnectionsAuthorizationSession> = postEvent(sessionId, events)

    suspend operator fun invoke(
        sessionId: String,
        event: AuthSessionEvent
    ): Result<FinancialConnectionsAuthorizationSession> = postEvent(sessionId, listOf(event))

    private suspend fun postEvent(
        sessionId: String,
        events: List<AuthSessionEvent>
    ) = kotlin.runCatching {
        repository.postAuthorizationSessionEvent(
            clientTimestamp = Date(),
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            sessionId = sessionId,
            authSessionEvents = events
        )
    }.onFailure {
        logger.error("error posting auth session event", it)
    }
}
