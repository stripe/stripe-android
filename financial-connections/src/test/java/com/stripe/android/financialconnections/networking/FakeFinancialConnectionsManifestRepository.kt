package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import java.util.Date

internal class FakeFinancialConnectionsManifestRepository : FinancialConnectionsManifestRepository {

    var getSynchronizeSessionResponseProvider: () -> SynchronizeSessionResponse =
        { ApiKeyFixtures.syncResponse() }
    var markConsentAcquiredProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }
    var postAuthorizationSessionProvider: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }
    var cancelAuthorizationSessionProvider: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }
    var postMarkLinkingMoreAccountsProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }
    var postAuthSessionEvent: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }

    override suspend fun getOrSynchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String,
        attestationInitialized: Boolean,
        reFetchCondition: (SynchronizeSessionResponse) -> Boolean
    ): SynchronizeSessionResponse = getSynchronizeSessionResponseProvider()

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest = markConsentAcquiredProvider()

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        applicationId: String,
        institution: FinancialConnectionsInstitution
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()

    override suspend fun postAuthorizationSessionEvent(
        clientSecret: String,
        clientTimestamp: Date,
        sessionId: String,
        authSessionEvents: List<AuthSessionEvent>
    ): FinancialConnectionsAuthorizationSession = postAuthSessionEvent()

    override suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String?
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()

    override suspend fun postMarkLinkingMoreAccounts(
        clientSecret: String
    ): FinancialConnectionsSessionManifest = postMarkLinkingMoreAccountsProvider()

    override suspend fun cancelAuthorizationSession(
        clientSecret: String,
        sessionId: String
    ): FinancialConnectionsAuthorizationSession = cancelAuthorizationSessionProvider()

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
        selectedAccounts: Set<String>?,
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
    ) = Unit
}
