package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import javax.inject.Inject

internal class ConfirmVerification @Inject constructor(
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
        verificationCode: String
    ): ConsumerSession = requireNotNull(
        consumerSessionRepository.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode
        )
    )
}
