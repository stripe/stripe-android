package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.ApiKeyFixtures
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
        institutionId: String
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()

    override suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        publicToken: String?
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()
}
