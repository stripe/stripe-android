package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState.Verified
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType.SignUp
import getRedactedPhoneNumber
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal const val KeyConsumerSession = "ConsumerSession"

@Parcelize
internal data class CachedConsumerSession(
    val emailAddress: String,
    val phoneNumber: String,
    val unredactedPhoneNumber: String?,
    val clientSecret: String,
    val publishableKey: String?,
    val isVerified: Boolean,
) : Parcelable

internal fun interface ConsumerSessionProvider {
    fun provideConsumerSession(): CachedConsumerSession?
}

internal interface ConsumerSessionRepository : ConsumerSessionProvider {
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
) : ConsumerSessionRepository {

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
        unredactedPhoneNumber = unredactedPhoneNumber,
        clientSecret = clientSecret,
        publishableKey = publishableKey,
        isVerified = verificationSessions.any { it.state == Verified || it.type == SignUp },
    )
}
