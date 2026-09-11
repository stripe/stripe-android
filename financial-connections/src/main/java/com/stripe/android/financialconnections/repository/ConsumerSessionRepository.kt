package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.FinancialConnectionsConsumer
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState.Verified
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType.SignUp
import getRedactedPhoneNumber
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal const val KeyConsumerSession = "ConsumerSession"

internal typealias CachedConsumerSession = FinancialConnectionsConsumer

internal fun interface ConsumerSessionProvider {
    fun provideConsumerSession(): CachedConsumerSession?
}

internal interface ConsumerSessionRepository : ConsumerSessionProvider {
    val consumerSessionFlow: StateFlow<CachedConsumerSession?>

    fun storeNewConsumerSession(
        consumerSession: ConsumerSession?,
        publishableKey: String?,
    )

    fun updateConsumerSession(
        consumerSession: ConsumerSession,
    )
}

internal class RealConsumerSessionRepository @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    configuration: FinancialConnectionsSheetConfiguration,
) : ConsumerSessionRepository {

    init {
        if (savedStateHandle.contains(KeyConsumerSession).not()) {
            savedStateHandle[KeyConsumerSession] = configuration.existingConsumer
        }
    }

    override val consumerSessionFlow: StateFlow<CachedConsumerSession?> =
        savedStateHandle.getStateFlow(key = KeyConsumerSession, initialValue = null)

    override fun provideConsumerSession(): CachedConsumerSession? {
        return savedStateHandle[KeyConsumerSession]
    }

    override fun storeNewConsumerSession(
        consumerSession: ConsumerSession?,
        publishableKey: String?,
    ) {
        savedStateHandle[KeyConsumerSession] = consumerSession?.toCached(publishableKey)
    }

    override fun updateConsumerSession(consumerSession: ConsumerSession) {
        val existingSession = provideConsumerSession()
        val publishableKey = existingSession?.publishableKey
        savedStateHandle[KeyConsumerSession] = consumerSession.toCached(publishableKey)
    }

    private fun ConsumerSession.toCached(
        publishableKey: String?,
    ) = CachedConsumerSession(
        emailAddress = emailAddress,
        phoneNumber = getRedactedPhoneNumber(),
        clientSecret = clientSecret,
        publishableKey = publishableKey,
        isVerified = verificationSessions.any { it.state == Verified || it.type == SignUp },
        linkBrand = linkBrand,
    )
}
