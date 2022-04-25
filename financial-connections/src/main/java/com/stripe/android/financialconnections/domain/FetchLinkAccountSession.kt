package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.LinkedAccount
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.model.ListLinkedAccountParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchLinkAccountSession @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository
) {

    /**
     * Fetches the link account session with all linked accounts via pagination
     *
     * @param clientSecret the link account session client secret
     *
     * @return LinkAccountSession with all linked accounts
     */
    suspend operator fun invoke(clientSecret: String): FinancialConnectionsSession {
        val linkAccountSession = financialConnectionsRepository.getLinkAccountSession(clientSecret)
        if (linkAccountSession.linkedAccounts.hasMore) {
            val accounts = mutableListOf<LinkedAccount>()
            accounts.addAll(linkAccountSession.linkedAccounts.linkedAccounts)

            var nextLinkedAccountList = financialConnectionsRepository.getLinkedAccounts(
                ListLinkedAccountParams(clientSecret, accounts.last().id)
            )
            accounts.addAll(nextLinkedAccountList.linkedAccounts)

            while (nextLinkedAccountList.hasMore && accounts.size < FinancialConnectionsSheetViewModel.MAX_ACCOUNTS) {
                nextLinkedAccountList = financialConnectionsRepository.getLinkedAccounts(
                    ListLinkedAccountParams(clientSecret, accounts.last().id)
                )
                accounts.addAll(nextLinkedAccountList.linkedAccounts)
            }

            return FinancialConnectionsSession(
                id = linkAccountSession.id,
                clientSecret = linkAccountSession.clientSecret,
                linkedAccounts = LinkedAccountList(
                    linkedAccounts = accounts,
                    hasMore = nextLinkedAccountList.hasMore,
                    url = nextLinkedAccountList.url,
                    count = accounts.size,
                    totalCount = nextLinkedAccountList.totalCount
                ),
                bankAccountToken = null, // Token should not be exposed on regular sessions.
                livemode = linkAccountSession.livemode
            )
        }
        return linkAccountSession
    }
}
