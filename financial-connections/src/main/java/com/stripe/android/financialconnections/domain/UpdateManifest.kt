package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * After step completion, an updated [FinancialConnectionsSessionManifest] is received from
 * the API. This usecase can be triggered to update the manifest instance across the flow.
 *
 * Each step is responsible for consuming manifest updates as needed.
 */
@Singleton
internal class UpdateManifest @Inject constructor() {
    val flow = MutableSharedFlow<FinancialConnectionsSessionManifest>()

    suspend operator fun invoke(manifest: FinancialConnectionsSessionManifest) {
        flow.emit(manifest)
    }
}
