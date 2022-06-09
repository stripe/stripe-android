package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class PostAuthorizationSession @Inject constructor(
    val repository: FinancialConnectionsRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        institutionId: String
    ): FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession {
        return repository.postAuthorizationSession(
            configuration.financialConnectionsSessionClientSecret,
            institutionId = institutionId
        )
    }
}
