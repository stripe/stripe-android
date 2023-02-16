package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

/**
 * Use case to update the local cached selected accounts with new data.
 */
internal class UpdateSelectedAccounts @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository
) {

    suspend operator fun invoke(
        selectedAccounts: List<PartnerAccount>
    ) {
        repository.updateCachedAccounts(selectedAccounts)
    }
}
