package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import javax.inject.Inject

internal class GetCachedConsumerSession @Inject constructor(
    private val repository: FinancialConnectionsConsumerSessionRepository,
) {

    suspend operator fun invoke(): CachedConsumerSession? {
        return repository.getCachedConsumerSession()
    }
}
