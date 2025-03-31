package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

internal class FetchNetworkedAccounts @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(
        consumerSessionClientSecret: String,
    ): NetworkedAccountsList = repository.getNetworkedAccounts(
        clientSecret = configuration.financialConnectionsSessionClientSecret,
        consumerSessionClientSecret = consumerSessionClientSecret
    )
}
