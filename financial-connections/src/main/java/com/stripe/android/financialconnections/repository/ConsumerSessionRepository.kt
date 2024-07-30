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

internal interface ConsumerSessionRepository {
    fun provideConsumerSession(): CachedConsumerSession?
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
        val cachedSession = consumerSession?.let { session ->
            CachedConsumerSession(
                emailAddress = session.emailAddress,
                phoneNumber = session.getRedactedPhoneNumber(),
                clientSecret = session.clientSecret,
                publishableKey = session.publishableKey,
                isVerified = session.verificationSessions.any {
                    it.state == ConsumerSession.VerificationSession.SessionState.Verified
                },
            )
        }
        savedStateHandle[KeyConsumerSession] = cachedSession
    }
}
