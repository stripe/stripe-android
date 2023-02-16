package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import javax.inject.Inject

internal class GetCachedConsumerSession @Inject constructor(
    val repository: FinancialConnectionsConsumerSessionRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(): ConsumerSession? {
        return repository.getCachedConsumerSession()
    }
}
