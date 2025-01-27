package com.stripe.android.financialconnections.domain

import android.app.Application
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationEndpoint
import com.stripe.android.financialconnections.features.error.toAttestationErrorIfApplicable
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.EmailSource
import javax.inject.Inject

internal class LookupAccount @Inject constructor(
    private val application: Application,
    private val requestIntegrityToken: RequestIntegrityToken,
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(
        email: String,
        emailSource: EmailSource,
        verifiedFlow: Boolean,
        sessionId: String,
        pane: Pane
    ): ConsumerSessionLookup {
        return runCatching {
            if (verifiedFlow) {
                val token = requestIntegrityToken(pane = pane, endpoint = AttestationEndpoint.LOOKUP)
                requireNotNull(
                    consumerSessionRepository.mobileLookupConsumerSession(
                        email = email.lowercase().trim(),
                        emailSource = emailSource,
                        verificationToken = token,
                        appId = application.packageName,
                        sessionId = sessionId
                    )
                )
            } else {
                requireNotNull(
                    consumerSessionRepository.postConsumerSession(
                        email = email.lowercase().trim(),
                        clientSecret = configuration.financialConnectionsSessionClientSecret
                    )
                )
            }
        }.getOrElse { throwable ->
            throw throwable.toAttestationErrorIfApplicable(prefilledEmail = email)
        }
    }
}
