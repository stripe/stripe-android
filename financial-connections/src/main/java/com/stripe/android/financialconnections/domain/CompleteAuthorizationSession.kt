package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class CompleteAuthorizationSession @Inject constructor(
    private val coordinator: NativeAuthFlowCoordinator,
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        authorizationSessionId: String
    ): FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession {
        return repository.completeAuthorizationSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            sessionId = authorizationSessionId,
            publicToken = null
        ).also { coordinator().emit(Message.ClearPartnerWebAuth) }
    }
}
