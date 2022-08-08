package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

/**
 * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
 * as well as the success and cancel callback URLs to verify.
 */
internal class GenerateFinancialConnectionsSessionManifest @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository
) {

    suspend operator fun invoke(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest {
        return financialConnectionsRepository.generateFinancialConnectionsSessionManifest(
            clientSecret = clientSecret,
            applicationId = applicationId
        )
    }
}
