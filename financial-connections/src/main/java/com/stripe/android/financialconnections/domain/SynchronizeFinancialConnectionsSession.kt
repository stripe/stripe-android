package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

/**
 * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
 * as well as the success and cancel callback URLs to verify.
 */
internal class SynchronizeFinancialConnectionsSession @Inject constructor(
    val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val financialConnectionsRepository: FinancialConnectionsManifestRepository
) {

    suspend operator fun invoke(): SynchronizeSessionResponse {
        return financialConnectionsRepository.synchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
        )
    }
}
