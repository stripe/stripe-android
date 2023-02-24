package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

/**
 * Use case to update the local cached manifest with new data.
 */
internal class UpdateLocalManifest @Inject constructor(
    val repository: FinancialConnectionsManifestRepository
) {

    operator fun invoke(
        block: (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
    ) {
        repository.updateLocalManifest(block)
    }
}
