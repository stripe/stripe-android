package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class DisableNetworking @Inject constructor(
    private val configuration: FinancialConnectionsSheetConfiguration,
    private val repository: FinancialConnectionsManifestRepository,
) {

    suspend operator fun invoke(
        clientSuggestedNextPaneOnDisableNetworking: String?
    ): FinancialConnectionsSessionManifest {
        return repository.disableNetworking(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            clientSuggestedNextPaneOnDisableNetworking = clientSuggestedNextPaneOnDisableNetworking,
            disabledReason = null
        )
    }
}
