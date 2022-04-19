package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.LinkAccountSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

/**
 * Fetches the [LinkAccountSessionManifest] from the Stripe API to get the hosted auth flow URL
 * as well as the success and cancel callback URLs to verify.
 */
internal class GenerateLinkAccountSessionManifest @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsRepository,
) {

    suspend operator fun invoke(
        clientSecret: String,
        applicationId: String
    ): LinkAccountSessionManifest {
        return financialConnectionsRepository.generateLinkAccountSessionManifest(
            clientSecret = clientSecret,
            applicationId = applicationId
        )
    }
}
