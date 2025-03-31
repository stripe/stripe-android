package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class AcceptConsent @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(): FinancialConnectionsSessionManifest {
        return repository.markConsentAcquired(configuration.financialConnectionsSessionClientSecret)
    }
}
