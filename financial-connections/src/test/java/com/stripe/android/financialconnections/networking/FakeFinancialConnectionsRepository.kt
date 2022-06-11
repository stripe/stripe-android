package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.moreFinancialConnectionsAccountList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository

internal class FakeFinancialConnectionsRepository(
    private val manifest: FinancialConnectionsSessionManifest,
) : FinancialConnectionsRepository {

    var getFinancialConnectionsSessionResultProvider: () -> FinancialConnectionsSession =
        { financialConnectionsSessionWithNoMoreAccounts }
    var getAccountsResultProvider: () -> FinancialConnectionsAccountList = { moreFinancialConnectionsAccountList }

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList = getAccountsResultProvider()

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession = getFinancialConnectionsSessionResultProvider()

    override suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest = manifest

    override suspend fun getFinancialConnectionsSessionManifest(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun markConsentAcquired(clientSecret: String): FinancialConnectionsSessionManifest {
        TODO("Not yet implemented")
    }

    override suspend fun searchInstitutions(
        clientSecret: String,
        query: String
    ): InstitutionResponse {
        TODO("Not yet implemented")
    }

    override suspend fun featuredInstitutions(clientSecret: String): InstitutionResponse {
        TODO("Not yet implemented")
    }

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        institutionId: String
    ): FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession {
        TODO("Not yet implemented")
    }
}
