package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.ConsumerSession
import getRedactedPhoneNumber
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal const val KeyConsumerSession = "ConsumerSession"

@Parcelize
internal data class CachedConsumerSession(
    val emailAddress: String,
    val phoneNumber: String,
    val clientSecret: String,
    val publishableKey: String?,
    val isVerified: Boolean,
) : Parcelable

internal fun interface ConsumerSessionProvider {
    fun provideConsumerSession(): CachedConsumerSession?
}

internal interface ConsumerSessionRepository : ConsumerSessionProvider {
    fun storeConsumerSession(consumerSession: ConsumerSession?)
}

internal class RealConsumerSessionRepository @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ConsumerSessionRepository {

    override fun provideConsumerSession(): CachedConsumerSession? {
        return savedStateHandle[KeyConsumerSession]
    }

    override fun storeConsumerSession(
        consumerSession: ConsumerSession?,
    ) {
        savedStateHandle[KeyConsumerSession] = consumerSession?.toCached()
    }

    private fun ConsumerSession.toCached() = CachedConsumerSession(
        emailAddress = emailAddress,
        phoneNumber = getRedactedPhoneNumber(),
        clientSecret = clientSecret,
        publishableKey = publishableKey,
        isVerified = verificationSessions.any {
            it.state == ConsumerSession.VerificationSession.SessionState.Verified
        },
    )
}
