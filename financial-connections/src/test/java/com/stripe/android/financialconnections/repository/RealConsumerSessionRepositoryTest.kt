package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsConsumer
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import org.junit.Test

class RealConsumerSessionRepositoryTest {

    @Test
    fun `Returns correct cached session that is verified`() {
        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
        )

        val store = createStore()
        store.storeNewConsumerSession(session, "pk_123")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = "pk_123",
                isVerified = true,
                linkBrand = null,
            )
        )
    }

    @Test
    fun `Returns correct cached session that is not verified`() {
        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = false,
        )

        val store = createStore()
        store.storeNewConsumerSession(session, "pk_123")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession).isEqualTo(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-1234",
                clientSecret = "abc_123",
                publishableKey = "pk_123",
                isVerified = false,
                linkBrand = null,
            )
        )
    }

    @Test
    fun `Keeps existing publishable key when storing updated consumer session`() {
        val store = createStore()

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
                linkBrand = null,
            )
        )
    }

    @Test
    fun `Overrides existing publishable key when storing new consumer session`() {
        val store = createStore()

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
                linkBrand = null,
            )
        )
    }

    @Test
    fun `storeNewConsumerSession persists linkBrand from ConsumerSession`() {
        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
            linkBrand = LinkBrand.Onelink,
        )

        val store = createStore()
        store.storeNewConsumerSession(session, "pk_123")

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession?.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `consumerSessionFlow emits null initially`() {
        val store = createStore()
        assertThat(store.consumerSessionFlow.value).isNull()
    }

    @Test
    fun `consumerSessionFlow emits updated session when stored`() {
        val store = createStore()

        val session = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
            linkBrand = LinkBrand.Link,
        )
        store.storeNewConsumerSession(session, "pk_123")

        val emitted = store.consumerSessionFlow.value
        assertThat(emitted?.linkBrand).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `updateConsumerSession updates linkBrand from new session`() {
        val store = createStore()

        val session1 = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
            linkBrand = LinkBrand.Link,
        )
        store.storeNewConsumerSession(session1, "pk_123")

        val session2 = makeConsumerSession(
            clientSecret = "abc_123",
            isVerified = true,
            linkBrand = LinkBrand.Onelink,
        )
        store.updateConsumerSession(session2)

        val cachedSession = store.provideConsumerSession()
        assertThat(cachedSession?.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `Seeds existing consumer from configuration`() {
        val existingConsumer = FinancialConnectionsConsumer(
            emailAddress = "email@email.com",
            phoneNumber = "(•••) •••-1234",
            clientSecret = "consumer_secret",
            publishableKey = "pk_consumer",
            isVerified = true,
            linkBrand = LinkBrand.Link,
        )

        val store = createStore(existingConsumer = existingConsumer)

        assertThat(store.provideConsumerSession()).isEqualTo(existingConsumer)
        assertThat(store.consumerSessionFlow.value).isEqualTo(existingConsumer)
    }

    private fun createStore(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        existingConsumer: FinancialConnectionsConsumer? = null,
    ): RealConsumerSessionRepository {
        return RealConsumerSessionRepository(
            savedStateHandle = savedStateHandle,
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "fcsess_secret",
                publishableKey = "pk_merchant",
                hasRequestedDataPermissions = existingConsumer != null,
                existingConsumer = existingConsumer,
            ),
        )
    }

    private fun makeConsumerSession(
        clientSecret: String,
        isVerified: Boolean,
        linkBrand: LinkBrand? = null,
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
            linkBrand = linkBrand,
        )
    }
}
