package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.PollNetworkedAccounts.Companion.MAX_TRIES
import com.stripe.android.financialconnections.domain.PollNetworkedAccounts.Companion.POLLING_TIME_MS
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

/**
 * Polls for networked accounts.
 *
 * Will retry upon 202 backend responses every [POLLING_TIME_MS] up to [MAX_TRIES]
 */
internal class PollNetworkedAccounts @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
    ): PartnerAccountsList = retryOnException(
        times = MAX_TRIES,
        delayMilliseconds = POLLING_TIME_MS,
        retryCondition = { exception -> exception.shouldRetry }
    ) {
        repository.getNetworkedAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            consumerSessionClientSecret = consumerSessionClientSecret
        )
    }

    private companion object {
        private const val POLLING_TIME_MS = 2000L
        private const val MAX_TRIES = 10
    }
}
