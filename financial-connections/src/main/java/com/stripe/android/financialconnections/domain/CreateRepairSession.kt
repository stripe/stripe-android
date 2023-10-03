package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationRepairSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

internal class CreateRepairSession @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String
) {

    suspend operator fun invoke(
        coreAuthorization: String
    ): FinancialConnectionsAuthorizationRepairSession {
        return repository.repairSessionGenerateUrl(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
            coreAuthorization = coreAuthorization,
        )
    }
}
