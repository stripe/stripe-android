package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchFinancialConnectionsSession @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository
) {

    /**
     * Fetches the session with all the connected accounts via pagination
     *
     * @param clientSecret the [FinancialConnectionsSession] client secret
     *
     * @return [FinancialConnectionsSession] with all connected accounts
     */
    suspend operator fun invoke(clientSecret: String): FinancialConnectionsSession {
        val session = financialConnectionsRepository.getFinancialConnectionsSession(clientSecret)
        if (session.accounts.hasMore) {
            val accounts = mutableListOf<FinancialConnectionsAccount>()
            accounts.addAll(session.accounts.financialConnectionsAccounts)

            var nextAccountList = financialConnectionsRepository.getFinancialConnectionsAccounts(
                GetFinancialConnectionsAcccountsParams(clientSecret, accounts.last().id)
            )
            accounts.addAll(nextAccountList.financialConnectionsAccounts)

            while (nextAccountList.hasMore && accounts.size < FinancialConnectionsSheetViewModel.MAX_ACCOUNTS) {
                nextAccountList = financialConnectionsRepository.getFinancialConnectionsAccounts(
                    GetFinancialConnectionsAcccountsParams(clientSecret, accounts.last().id)
                )
                accounts.addAll(nextAccountList.financialConnectionsAccounts)
            }

            return FinancialConnectionsSession(
                id = session.id,
                clientSecret = session.clientSecret,
                accounts = FinancialConnectionsAccountList(
                    financialConnectionsAccounts = accounts,
                    hasMore = nextAccountList.hasMore,
                    url = nextAccountList.url,
                    count = accounts.size,
                    totalCount = nextAccountList.totalCount
                ),
                bankAccountToken = null, // Token should not be exposed on regular sessions.
                livemode = session.livemode
            )
        }
        return session
    }
}
