package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class CompleteFinancialConnectionsSession @Inject constructor(
    private val repository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(): FinancialConnectionsSession {
        return repository.postCompleteFinancialConnectionsSessions(
            clientSecret = configuration.financialConnectionsSessionClientSecret
        )
    }
}
