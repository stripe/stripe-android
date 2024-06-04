package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class AcceptConsent @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(): FinancialConnectionsSessionManifest {
        return repository.markConsentAcquired(configuration.financialConnectionsSessionClientSecret)
    }
}
