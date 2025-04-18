package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerSession
import org.junit.Test

class RealConsumerSessionRepositoryTest {

    @Test
    fun `Returns correct cached session that is verified`() {
        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
        )

        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())
        store.storeNewConsumerSession(session, "pk_123")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = "pk_123",
                isVerified = true,
            )
        )
    }

    @Test
    fun `Returns correct cached session that is not verified`() {
        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = false,
        )

        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())
        store.storeNewConsumerSession(session, "pk_123")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = "pk_123",
                isVerified = false,
            )
        )
    }

    @Test
    fun `Keeps existing publishable key when storing updated consumer session`() {
        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())

        val session1 = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = false,
        )

        val session2 = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
        )

        store.storeNewConsumerSession(session1, publishableKey = "pk_123")
        store.updateConsumerSession(session2)

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = "pk_123",
                isVerified = true,
            )
        )
    }

    @Test
    fun `Overrides existing publishable key when storing new consumer session`() {
        val store = RealConsumerSessionRepository(savedStateHandle = SavedStateHandle())

        val session1 = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = false,
        )

        val session2 = makeConsumerSession(
            clientSecret = "abc_456",
            isVerified = false,
        )

        store.storeNewConsumerSession(session1, publishableKey = "pk_123")
        store.storeNewConsumerSession(session2, publishableKey = "pk_456")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_456",
                publishableKey = "pk_456",
                isVerified = false,
            )
        )
    }

    private fun makeConsumerSession(
        clientSecret: String,
        isVerified: Boolean
    ): ConsumerSession {
        return ConsumerSession(
            clientSecret = clientSecret,
            emailAddress = "email@email.com",
            redactedFormattedPhoneNumber = "(***) ***-1234",
            redactedPhoneNumber = "******1234",
            unredactedPhoneNumber = null,
            phoneNumberCountry = null,
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Sms,
                    state = if (isVerified) {
                        ConsumerSession.VerificationSession.SessionState.Verified
                    } else {
                        ConsumerSession.VerificationSession.SessionState.Started
                    },
                ),
            ),
        )
    }
}
