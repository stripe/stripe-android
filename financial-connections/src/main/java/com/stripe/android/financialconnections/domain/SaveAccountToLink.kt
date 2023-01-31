package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class SaveAccountToLink @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val repository: FinancialConnectionsManifestRepository
) {

    suspend operator fun invoke(
        email: String,
        phoneNumber: String,
        selectAccounts: List<String>,
        country: String
    ): FinancialConnectionsSessionManifest {
        return repository.postSaveAccountsToLink(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            email = email,
            country = country,
            phoneNumber = phoneNumber,
            selectAccounts = selectAccounts
        )
    }
}
