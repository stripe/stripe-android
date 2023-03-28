package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionLookup
import javax.inject.Inject

internal class LookupAccount @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository
) {

    suspend operator fun invoke(
        email: String
    ): ConsumerSessionLookup = requireNotNull(
        consumerSessionRepository.lookupConsumerSession(
            email = email.lowercase().trim(),
        )
    )
}
