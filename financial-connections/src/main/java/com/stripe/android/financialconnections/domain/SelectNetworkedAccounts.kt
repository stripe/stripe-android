package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

internal class SelectNetworkedAccounts @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val repository: FinancialConnectionsAccountsRepository
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
        selectedAccountIds: Set<String>,
        consentAcquired: Boolean
    ): InstitutionResponse {
        return repository.postShareNetworkedAccounts(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountIds = selectedAccountIds,
            consentAcquired = consentAcquired
        )
    }
}
