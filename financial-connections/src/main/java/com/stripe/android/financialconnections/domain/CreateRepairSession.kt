package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationRepairSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class CreateRepairSession @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        coreAuthorization: String
    ): FinancialConnectionsAuthorizationRepairSession {
        return repository.repairSessionGenerateUrl(
            configuration.financialConnectionsSessionClientSecret,
            coreAuthorization = coreAuthorization,
        )
    }
}
