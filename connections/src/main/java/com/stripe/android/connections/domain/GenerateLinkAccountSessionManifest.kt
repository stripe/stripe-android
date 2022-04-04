package com.stripe.android.connections.domain

import com.stripe.android.connections.model.LinkAccountSessionManifest
import com.stripe.android.connections.repository.ConnectionsRepository
import javax.inject.Inject

/**
 * Fetches the [LinkAccountSessionManifest] from the Stripe API to get the hosted auth flow URL
 * as well as the success and cancel callback URLs to verify.
 */
internal class GenerateLinkAccountSessionManifest @Inject constructor(
    private val connectionsRepository: ConnectionsRepository,
) {

    suspend operator fun invoke(
        clientSecret: String,
        applicationId: String
    ): LinkAccountSessionManifest {
        return connectionsRepository.generateLinkAccountSessionManifest(
            clientSecret = clientSecret,
            applicationId = applicationId
        )
    }
}
