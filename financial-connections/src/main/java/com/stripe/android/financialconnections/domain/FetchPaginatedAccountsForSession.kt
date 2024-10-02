package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAccountsParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchPaginatedAccountsForSession @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository
) {

    /**
     * If the given session has paginated accounts that need to be fetched, fetches
     * them until no more accounts remain.
     *
     * @param session the [FinancialConnectionsSession] with potential paginated accounts.
     *
     * @return [FinancialConnectionsSession] with all connected accounts
     */
    suspend operator fun invoke(session: FinancialConnectionsSession): FinancialConnectionsSession {
        return if (session.accounts.hasMore) {
            val accounts = mutableListOf<FinancialConnectionsAccount>()
            accounts.addAll(session.accounts.data)

            var nextAccountList = financialConnectionsRepository.getFinancialConnectionsAccounts(
                GetFinancialConnectionsAccountsParams(session.clientSecret, accounts.last().id)
            )
            accounts.addAll(nextAccountList.data)

            while (nextAccountList.hasMore && accounts.size < FinancialConnectionsSheetViewModel.MAX_ACCOUNTS) {
                nextAccountList = financialConnectionsRepository.getFinancialConnectionsAccounts(
                    GetFinancialConnectionsAccountsParams(session.clientSecret, accounts.last().id)
                )
                accounts.addAll(nextAccountList.data)
            }

            FinancialConnectionsSession(
                id = session.id,
                clientSecret = session.clientSecret,
                accountsNew = FinancialConnectionsAccountList(
                    data = accounts,
                    hasMore = nextAccountList.hasMore,
                    url = nextAccountList.url,
                    count = accounts.size,
                    totalCount = nextAccountList.totalCount
                ),
                bankAccountToken = null, // Token should not be exposed on regular sessions.
                livemode = session.livemode
            )
        } else {
            session
        }
    }
}
