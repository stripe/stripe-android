package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import javax.inject.Inject

/**
 * Retrieves the current cached [ConsumerSession] instance.
 */
internal class GetConsumerSession @Inject constructor(
    val repository: FinancialConnectionsConsumerSessionRepository,
) {

    suspend operator fun invoke(): ConsumerSession? = repository.getCachedConsumerSession()
}
