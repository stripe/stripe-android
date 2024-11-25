package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

internal class SelectAccounts @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        selectedAccountIds: Set<String>,
        sessionId: String,
    ): PartnerAccountsList {
        return repository.postAuthorizationSessionSelectedAccounts(
            sessionId = sessionId,
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            selectAccounts = selectedAccountIds.toList(),
        )
    }
}
