package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationRepairSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class CompleteRepairSession @Inject constructor(
    private val coordinator: NativeAuthFlowCoordinator,
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        authRepairSessionId: String,
        coreAuthorization: String,
    ): FinancialConnectionsAuthorizationRepairSession {
        return repository.completeRepairSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            authRepairSessionId = authRepairSessionId,
            coreAuthorization = coreAuthorization,
        ).also { coordinator().emit(Message.ClearPartnerWebAuth) }
    }
}
