package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import javax.inject.Inject

internal fun interface AttachConsumerToLinkAccountSession {
    suspend operator fun invoke(
        consumerSessionClientSecret: String,
    )
}

internal class RealAttachConsumerToLinkAccountSession @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
) : AttachConsumerToLinkAccountSession {

    override suspend operator fun invoke(
        consumerSessionClientSecret: String,
    ) {
        consumerRepository.attachLinkConsumerToLinkAccountSession(
            consumerSessionClientSecret = consumerSessionClientSecret,
            clientSecret = configuration.financialConnectionsSessionClientSecret,
        )
    }
}
