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

    /**
     * @param emitEvents whether the backend should return live events to emit in the response.
     * events should just be emitted on the first sync call.
     */
    suspend operator fun invoke(
        emitEvents: Boolean
    ): SynchronizeSessionResponse {
        return financialConnectionsRepository.synchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
            // Sync session is called in a few places:
            // - when the flow starts -> we expect an "open" event.
            // - networking account picker (to retrieve the latest TextUpdate) -> no events expected
            // - after process kills, to restore the session. -> no events expected
            emitEvents = emitEvents
        )
    }
}
