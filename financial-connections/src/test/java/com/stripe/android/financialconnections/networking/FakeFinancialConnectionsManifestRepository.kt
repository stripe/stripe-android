package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository

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

    override suspend fun getOrFetchSynchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String
    ): SynchronizeSessionResponse = getSynchronizeSessionResponseProvider()

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest = markConsentAcquiredProvider()

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        applicationId: String,
        institution: FinancialConnectionsInstitution
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()

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

    override suspend fun synchronizeFinancialConnectionsSession(
        clientSecret: String,
        applicationId: String
    ): SynchronizeSessionResponse = getSynchronizeSessionResponseProvider()

    override fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) = Unit
}
