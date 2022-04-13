package com.stripe.android.connections.networking

import com.stripe.android.connections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import com.stripe.android.connections.model.LinkedAccountList
import com.stripe.android.connections.model.ListLinkedAccountParams
import com.stripe.android.connections.moreLinkedAccountList
import com.stripe.android.connections.repository.ConnectionsRepository

internal class FakeConnectionsRepository(
    private val manifest: LinkAccountSessionManifest,
) : ConnectionsRepository {

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
