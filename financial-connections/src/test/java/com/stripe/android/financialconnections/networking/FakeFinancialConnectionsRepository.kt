package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.moreFinancialConnectionsAccountList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository

internal class FakeFinancialConnectionsRepository : FinancialConnectionsRepository {

    var getFinancialConnectionsSessionResultProvider: () -> FinancialConnectionsSession =
        { financialConnectionsSessionWithNoMoreAccounts }
    var getAccountsResultProvider: () -> FinancialConnectionsAccountList =
        { moreFinancialConnectionsAccountList }
    var postAuthorizationSessionProvider: () -> FinancialConnectionsAuthorizationSession =
        { ApiKeyFixtures.authorizationSession() }

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList = getAccountsResultProvider()

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession = getFinancialConnectionsSessionResultProvider()

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        institutionId: String
    ): FinancialConnectionsAuthorizationSession = postAuthorizationSessionProvider()
}
