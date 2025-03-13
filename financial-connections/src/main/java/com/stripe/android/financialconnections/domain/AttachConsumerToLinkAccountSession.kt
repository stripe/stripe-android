package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import javax.inject.Inject

internal fun interface AttachConsumerToLinkAccountSession {
    suspend operator fun invoke(
        consumerSessionClientSecret: String,
    )
}

internal class RealAttachConsumerToLinkAccountSession @Inject constructor(
    private val configuration: FinancialConnectionsSheetConfiguration,
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
