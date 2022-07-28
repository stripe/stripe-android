package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import kotlinx.coroutines.delay
import java.net.HttpURLConnection.HTTP_ACCEPTED
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
        pollingTime: Long = POLLING_TIME_MS,
        maxTries: Int = MAX_TRIES
    ): MixedOAuthParams {
        var result: MixedOAuthParams?
        do result = getOAuthResults(session).fold(
            onSuccess = { it },
            onFailure = { error ->
                if (error.shouldRetry) {
                    delay(pollingTime)
                    null
                } else throw error
            }
        )
        while (result == null)
        return result
    }

    /**
     * @return null if should keep polling, actual result otherwise.
     */

    private suspend fun getOAuthResults(
        session: FinancialConnectionsAuthorizationSession
    ): Result<MixedOAuthParams> =
        kotlin.runCatching {
            repository.postAuthorizationSessionOAuthResults(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = session.id
            )
        }

    private val Throwable.shouldRetry: Boolean
        get() {
            val statusCode: Int? = (this as? StripeException)?.statusCode
            return statusCode == HTTP_ACCEPTED
        }

    companion object {
        private const val POLLING_TIME_MS = 3000L
        private const val MAX_TRIES = 10
    }
}
