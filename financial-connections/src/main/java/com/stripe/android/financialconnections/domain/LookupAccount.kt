package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionLookup
import javax.inject.Inject

internal class LookupAccount @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(
        email: String
    ): ConsumerSessionLookup = requireNotNull(
        consumerSessionRepository.lookupConsumerSession(
            email = email.lowercase().trim(),
            clientSecret = configuration.financialConnectionsSessionClientSecret
        )
    )
}
