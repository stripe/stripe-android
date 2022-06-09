package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * After step completion, an updated [FinancialConnectionsSessionManifest] is received from
 * the API. This usecase can be triggered to update the manifest instance across the flow.
 *
 * Each step is responsible for consuming manifest updates as needed.
 */
@Singleton
internal class ManifestFlow @Inject constructor() : ObserveManifestUpdates, UpdateManifest {
    val flow = MutableSharedFlow<FinancialConnectionsSessionManifest>()

    override suspend operator fun invoke(manifest: FinancialConnectionsSessionManifest) {
        flow.emit(manifest)
    }

    override fun invoke(): SharedFlow<FinancialConnectionsSessionManifest> {
        return flow
    }

}

/**
 * Only-read interface for [ManifestFlow]
 */
internal interface ObserveManifestUpdates {
    operator fun invoke(): SharedFlow<FinancialConnectionsSessionManifest>
}

/**
 * Write-only interface for [ManifestFlow]
 */
internal interface UpdateManifest {
    suspend operator fun invoke(manifest: FinancialConnectionsSessionManifest)
}