package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

/**
 * Retrieves the current cached [SynchronizeSessionResponse] instance, or fetches
 * it from backend if no cached version available.
 */
internal class GetOrFetchSync @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String,
) {

    suspend operator fun invoke(): SynchronizeSessionResponse {
        return repository.getOrFetchSynchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId
        )
    }
}
