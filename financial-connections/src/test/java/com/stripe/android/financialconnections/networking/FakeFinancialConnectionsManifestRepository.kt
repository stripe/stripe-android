package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository

internal class FakeFinancialConnectionsManifestRepository : FinancialConnectionsManifestRepository {

    var generateFinancialConnectionsSessionManifestProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }
    var getManifestProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }
    var markConsentAcquiredProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }
    var postAuthorizationSessionProvider: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }
    var cancelAuthorizationSessionProvider: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }
    var postMarkLinkingMoreAccountsProvider: () -> FinancialConnectionsSessionManifest =
        { ApiKeyFixtures.sessionManifest() }

    override suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest {
        return generateFinancialConnectionsSessionManifestProvider()
    }

    override suspend fun getOrFetchManifest(): FinancialConnectionsSessionManifest =
        getManifestProvider()

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

    override fun updateLocalManifest(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) = Unit
}
