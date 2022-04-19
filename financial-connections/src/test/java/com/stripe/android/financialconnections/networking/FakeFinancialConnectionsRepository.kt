package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.financialconnections.model.LinkAccountSessionManifest
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.model.ListLinkedAccountParams
import com.stripe.android.financialconnections.moreLinkedAccountList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository

internal class FakeFinancialConnectionsRepository(
    private val manifest: LinkAccountSessionManifest,
) : FinancialConnectionsRepository {

    var getLinkAccountSessionResultProvider: () -> LinkAccountSession =
        { linkAccountSessionWithNoMoreAccounts }
    var getLinkedAccountsResultProvider: () -> LinkedAccountList = { moreLinkedAccountList }

    override suspend fun getLinkedAccounts(
        listLinkedAccountParams: ListLinkedAccountParams
    ): LinkedAccountList = getLinkedAccountsResultProvider()

    override suspend fun getLinkAccountSession(
        clientSecret: String
    ): LinkAccountSession = getLinkAccountSessionResultProvider()

    override suspend fun generateLinkAccountSessionManifest(
        clientSecret: String,
        applicationId: String
    ): LinkAccountSessionManifest = manifest
}
