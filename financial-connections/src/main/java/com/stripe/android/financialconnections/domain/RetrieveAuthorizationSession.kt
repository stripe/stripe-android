package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class RetrieveAuthorizationSession @Inject constructor(
    private val coordinator: NativeAuthFlowCoordinator,
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        authorizationSessionId: String
    ): FinancialConnectionsAuthorizationSession {
        return repository.retrieveAuthorizationSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            sessionId = authorizationSessionId,
            // Retrieve session is currently just called after returning from institution
            // auth, and we expect a "institution_authorized" event.
            // If we ever call this method in other places, this value should be passed in.
            emitEvents = true
        ).also { coordinator().emit(Message.ClearPartnerWebAuth) }
    }
}
