package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerSession
import org.junit.Test

class RealConsumerSessionRepositoryTest {

    @Test
    fun `Returns correct cached session that is verified`() {
        val session = ConsumerSession(
            clientSecret = "abc_123",
            emailAddress = "email@email.com",
            redactedFormattedPhoneNumber = "(***) ***-1234",
            redactedPhoneNumber = "******1234",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Sms,
                    state = ConsumerSession.VerificationSession.SessionState.Verified,
                ),
            ),
        )

        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())
        store.storeConsumerSession(session)

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = null,
                isVerified = true,
            )
        )
    }

    @Test
    fun `Returns correct cached session that is not verified`() {
        val session = ConsumerSession(
            clientSecret = "abc_123",
            emailAddress = "email@email.com",
            redactedFormattedPhoneNumber = "(***) ***-1234",
            redactedPhoneNumber = "******1234",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Sms,
                    state = ConsumerSession.VerificationSession.SessionState.Started,
                ),
            ),
        )

        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())
        store.storeConsumerSession(session)

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = null,
                isVerified = false,
            )
        )
    }
}
