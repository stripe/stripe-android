package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import java.util.Date

internal abstract class AbsFinancialConnectionsManifestRepository : FinancialConnectionsManifestRepository {

    override suspend fun markConsentAcquired(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        applicationId: String,
        institution: FinancialConnectionsInstitution
    ): FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }

    override suspend fun postAuthorizationSessionEvent(
        clientSecret: String,
        clientTimestamp: Date,
        sessionId: String,
        authSessionEvents: List<AuthSessionEvent>
    ): FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }

    override suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String?
    ): FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }

    override suspend fun postMarkLinkingMoreAccounts(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun cancelAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }

    override suspend fun postSaveAccountsToLink(
        clientSecret: String,
        email: String?,
        country: String?,
        locale: String?,
        phoneNumber: String?,
        consumerSessionClientSecret: String?,
        selectedAccounts: Set<String>?
    ): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun disableNetworking(
        clientSecret: String,
        disabledReason: String?,
        clientSuggestedNextPaneOnDisableNetworking: String?
    ): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun postMarkLinkVerified(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun postMarkLinkStepUpVerified(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getOrSynchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String,
        supportsAppVerification: Boolean,
        reFetchCondition: (SynchronizeSessionResponse) -> Boolean
    ): SynchronizeSessionResponse {
        TODO("Not yet implemented")
    }
}
