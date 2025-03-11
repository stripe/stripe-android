package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class CancelAuthorizationSession @Inject constructor(
    private val coordinator: NativeAuthFlowCoordinator,
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(
        authorizationSessionId: String
    ): FinancialConnectionsAuthorizationSession {
        return repository.cancelAuthorizationSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            sessionId = authorizationSessionId
        ).also { coordinator().emit(Message.ClearPartnerWebAuth) }
    }
}
