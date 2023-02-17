package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

internal class SelectNetworkedAccount @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val repository: FinancialConnectionsAccountsRepository
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
        selectedAccountId: String,
    ): InstitutionResponse {
        return repository.postShareNetworkedAccount(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountId = selectedAccountId
        )
    }
}
