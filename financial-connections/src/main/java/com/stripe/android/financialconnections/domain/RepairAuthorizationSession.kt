package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

internal class RepairAuthorizationSession @Inject constructor(
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String,
) {

    suspend operator fun invoke(
        coreAuthorization: String
    ): FinancialConnectionsAuthorizationSession {
        return repository.repairAuthorizationSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            coreAuthorization = coreAuthorization,
            applicationId = applicationId,
        )
    }
}
