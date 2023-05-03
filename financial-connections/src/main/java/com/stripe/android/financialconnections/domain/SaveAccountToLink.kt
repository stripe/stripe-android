package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import java.util.Locale
import javax.inject.Inject

internal class SaveAccountToLink @Inject constructor(
    private val locale: Locale?,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val repository: FinancialConnectionsManifestRepository
) {

    suspend fun new(
        email: String,
        phoneNumber: String,
        selectedAccounts: List<String>,
        country: String
    ): FinancialConnectionsSessionManifest {
        return repository.postSaveAccountsToLink(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            email = email,
            country = country,
            phoneNumber = phoneNumber,
            locale = (locale ?: Locale.getDefault()).toLanguageTag(),
            consumerSessionClientSecret = null,
            selectedAccounts = selectedAccounts
        )
    }

    suspend fun existing(
        consumerSessionClientSecret: String,
        selectedAccounts: List<String>,
    ): FinancialConnectionsSessionManifest {
        return repository.postSaveAccountsToLink(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            email = null,
            country = null,
            phoneNumber = null,
            locale = null,
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccounts = selectedAccounts
        )
    }
}
