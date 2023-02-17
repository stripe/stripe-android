package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

/**
 * Gets cached partner accounts.
 */
internal class GetCachedAccounts @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(): List<PartnerAccount> {
        return requireNotNull(repository.getCachedAccounts())
    }
}
