package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

/**
 * Polls accounts from backend after authorization session completes.
 *
 * Will retry
 */
internal class PollAuthorizationSessionOAuthResults @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        session: FinancialConnectionsAuthorizationSession,
    ): MixedOAuthParams {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            repository.postAuthorizationSessionOAuthResults(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = session.id
            )
        }
    }

    companion object {
        private const val POLLING_TIME_MS = 2000L
        private const val MAX_TRIES = 10
    }
}
