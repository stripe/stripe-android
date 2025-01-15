package com.stripe.android.financialconnections.domain

import android.app.Application
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.EmailSource
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject

internal class LookupAccount @Inject constructor(
    private val application: Application,
    private val integrityRequestManager: IntegrityRequestManager,
    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository,
    val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(
        email: String,
        emailSource: EmailSource,
        verifiedFlow: Boolean,
        sessionId: String
    ): ConsumerSessionLookup {
        return if (verifiedFlow) {
            requireNotNull(
                consumerSessionRepository.mobileLookupConsumerSession(
                    email = email.lowercase().trim(),
                    emailSource = emailSource,
                    verificationToken = integrityRequestManager.requestToken().getOrThrow(),
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
    }
}
