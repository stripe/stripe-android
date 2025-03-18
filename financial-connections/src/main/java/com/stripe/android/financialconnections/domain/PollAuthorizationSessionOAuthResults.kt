package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.utils.PollTimingOptions
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Polls OAuth results from backend after user finishes authorization on web browser.
 *
 * Will retry upon 202 backend responses.
 */
internal class PollAuthorizationSessionOAuthResults @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(
        session: FinancialConnectionsAuthorizationSession
    ): MixedOAuthParams {
        return retryOnException(
            PollTimingOptions(
                initialDelayMs = 0,
                maxNumberOfRetries = 300, // Stripe.js has 600 second timeout, 600 / 2 = 300 retries
                retryInterval = 2.seconds.inWholeMilliseconds
            ),
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            repository.postAuthorizationSessionOAuthResults(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = session.id
            )
        }
    }
}
