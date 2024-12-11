package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class PostAuthSessionEvent @Inject constructor(
    private val repository: FinancialConnectionsManifestRepository,
    private val logger: Logger,
    private val configuration: FinancialConnectionsSheet.Configuration,
) {

    @OptIn(DelicateCoroutinesApi::class)
    operator fun invoke(
        sessionId: String,
        events: List<AuthSessionEvent>
    ) {
//        GlobalScope.launch(Dispatchers.IO) {
//            runCatching {
//                repository.postAuthorizationSessionEvent(
//                    clientTimestamp = Date(),
//                    clientSecret = configuration.financialConnectionsSessionClientSecret,
//                    sessionId = sessionId,
//                    authSessionEvents = events
//                )
//            }.onFailure {
//                logger.error("error posting auth session event", it)
//            }
//        }
    }

    operator fun invoke(
        sessionId: String,
        event: AuthSessionEvent
    ) {
        invoke(sessionId, listOf(event))
    }
}
