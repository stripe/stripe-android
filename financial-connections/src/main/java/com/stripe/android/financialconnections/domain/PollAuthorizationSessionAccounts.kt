package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

/**
 * Polls accounts from backend after authorization session completes.
 *
 * Will retry upon 202 backend responses every [POLLING_TIME_MS] up to [MAX_TRIES]
 */
internal class PollAuthorizationSessionAccounts @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        sessionId: String,
    ): PartnerAccountsList {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            repository.postAuthorizationSessionAccounts(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = sessionId
            )
        }
    }

    private companion object {
        private const val POLLING_TIME_MS = 2000L
        private const val MAX_TRIES = 10
    }
}
