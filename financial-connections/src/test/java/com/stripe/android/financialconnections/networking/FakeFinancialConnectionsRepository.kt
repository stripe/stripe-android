package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.model.ListLinkedAccountParams
import com.stripe.android.financialconnections.moreLinkedAccountList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository

internal class FakeFinancialConnectionsRepository(
    private val manifest: FinancialConnectionsSessionManifest,
) : FinancialConnectionsRepository {

    var getFinancialConnectionsSessionResultProvider: () -> FinancialConnectionsSession =
        { financialConnectionsSessionWithNoMoreAccounts }
    var getLinkedAccountsResultProvider: () -> LinkedAccountList = { moreLinkedAccountList }

    override suspend fun getLinkedAccounts(
        listLinkedAccountParams: ListLinkedAccountParams
    ): LinkedAccountList = getLinkedAccountsResultProvider()

    override suspend fun getLinkAccountSession(
        clientSecret: String
    ): FinancialConnectionsSession = getFinancialConnectionsSessionResultProvider()

    override suspend fun generateLinkAccountSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest = manifest
}
