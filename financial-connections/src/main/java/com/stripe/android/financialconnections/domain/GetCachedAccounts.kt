package com.stripe.android.financialconnections.domain

import android.os.Parcelable
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Gets cached partner accounts.
 */
internal class GetCachedAccounts @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository,
    val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(): List<CachedPartnerAccount> {
        return repository.getCachedAccounts() ?: emptyList()
    }
}

@Parcelize
internal data class CachedPartnerAccount(
    val id: String,
    val linkedAccountId: String?,
) : Parcelable

internal fun List<PartnerAccount>.toCachedPartnerAccounts(): List<CachedPartnerAccount> {
    return map { CachedPartnerAccount(id = it.id, linkedAccountId = it.linkedAccountId) }
}
