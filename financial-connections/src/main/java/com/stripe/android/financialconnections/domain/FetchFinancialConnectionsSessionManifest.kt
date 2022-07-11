package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class FetchFinancialConnectionsSessionManifest @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
    ): FinancialConnectionsSessionManifest {
        return financialConnectionsRepository.getFinancialConnectionsSessionManifest(
            clientSecret = configuration.financialConnectionsSessionClientSecret
        )
    }
}
