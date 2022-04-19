package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.ConnectionsSheetViewModel
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.financialconnections.model.LinkedAccount
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.model.ListLinkedAccountParams
import com.stripe.android.financialconnections.repository.ConnectionsRepository
import javax.inject.Inject

internal class FetchLinkAccountSession @Inject constructor(
    private val connectionsRepository: ConnectionsRepository
) {

    /**
     * Fetches the link account session with all linked accounts via pagination
     *
     * @param clientSecret the link account session client secret
     *
     * @return LinkAccountSession with all linked accounts
     */
    suspend operator fun invoke(clientSecret: String): LinkAccountSession {
        val linkAccountSession = connectionsRepository.getLinkAccountSession(clientSecret)
        if (linkAccountSession.linkedAccounts.hasMore) {
            val accounts = mutableListOf<LinkedAccount>()
            accounts.addAll(linkAccountSession.linkedAccounts.linkedAccounts)

            var nextLinkedAccountList = connectionsRepository.getLinkedAccounts(
                ListLinkedAccountParams(clientSecret, accounts.last().id)
            )
            accounts.addAll(nextLinkedAccountList.linkedAccounts)

            while (nextLinkedAccountList.hasMore && accounts.size < ConnectionsSheetViewModel.MAX_ACCOUNTS) {
                nextLinkedAccountList = connectionsRepository.getLinkedAccounts(
                    ListLinkedAccountParams(clientSecret, accounts.last().id)
                )
                accounts.addAll(nextLinkedAccountList.linkedAccounts)
            }

            return LinkAccountSession(
                id = linkAccountSession.id,
                clientSecret = linkAccountSession.clientSecret,
                linkedAccounts = LinkedAccountList(
                    linkedAccounts = accounts,
                    hasMore = nextLinkedAccountList.hasMore,
                    url = nextLinkedAccountList.url,
                    count = accounts.size,
                    totalCount = nextLinkedAccountList.totalCount
                ),
                livemode = linkAccountSession.livemode
            )
        }
        return linkAccountSession
    }
}
