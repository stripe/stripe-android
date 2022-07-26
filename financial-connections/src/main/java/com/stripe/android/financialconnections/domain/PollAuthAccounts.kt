package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import kotlinx.coroutines.delay
import java.net.HttpURLConnection.HTTP_ACCEPTED
import javax.inject.Inject

/**
 * Polls accounts from backend after authorization session completes.
 *
 * Will retry
 */
internal class PollAuthAccounts @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        session: FinancialConnectionsAuthorizationSession,
        pollingTime: Long = POLLING_TIME_MS
    ): PartnerAccountsList {
        var result: PartnerAccountsList?
        do result = poll(session, pollingTime) while (result == null)
        return result
    }

    /**
     * @return null if should keep polling, actual result otherwise.
     */
    private suspend fun poll(
        session: FinancialConnectionsAuthorizationSession,
        pollingTime: Long
    ): PartnerAccountsList? {
        return getAccounts(session).fold(
            onSuccess = { accounts -> accounts },
            onFailure = { exception ->
                if (exception.shouldRetry) {
                    delay(pollingTime)
                    return null
                } else {
                    throw exception
                }
            }
        )
    }

    private suspend fun getAccounts(session: FinancialConnectionsAuthorizationSession) =
        kotlin.runCatching {
            repository.getAuthorizationSessionAccounts(
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
    }
}
