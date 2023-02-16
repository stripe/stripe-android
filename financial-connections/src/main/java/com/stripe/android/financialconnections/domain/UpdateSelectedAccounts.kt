package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
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
        repository.updateCachedAccounts(
            PartnerAccountsList(
                data = selectedAccounts,
                hasMore = false,
                count = selectedAccounts.count(),
                nextPane = FinancialConnectionsSessionManifest.Pane.SUCCESS,
                url = ""
            )
        )
    }
}
