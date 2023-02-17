package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import javax.inject.Inject

internal class StartVerification @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
        type: VerificationType
    ): ConsumerSession = requireNotNull(
        consumerSessionRepository.startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            type = type,
        )
    )
}
