package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LinkAccountManagerTest {
    private val linkRepository = mock<LinkRepository>()
    private val linkEventsReporter = mock<LinkEventsReporter>()

    private val verifiedSession = mock<ConsumerSession.VerificationSession>().apply {
        whenever(type).thenReturn(ConsumerSession.VerificationSession.SessionType.Sms)
        whenever(state).thenReturn(ConsumerSession.VerificationSession.SessionState.Verified)
    }
    private val mockConsumerSession = mock<ConsumerSession>().apply {
        whenever(clientSecret).thenReturn(CLIENT_SECRET)
        whenever(verificationSessions).thenReturn(listOf(verifiedSession))
        whenever(publishableKey).thenReturn(PUBLISHABLE_KEY)
    }

    @Test
    fun `When cookie exists and network call fails then account status is Error`() = runSuspendTest {
        val accountManager = accountManager(EMAIL)
        accountManager.authSessionCookie = "cookie"
        whenever(linkRepository.lookupConsumer(anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(Exception()))

        assertThat(accountManager.accountStatus.first()).isEqualTo(AccountStatus.Error)
    }

    @Test
    fun `When customerEmail is set in arguments then it is looked up`() = runSuspendTest {
        assertThat(accountManager(EMAIL).accountStatus.first()).isEqualTo(AccountStatus.Verified)

        verify(linkRepository).lookupConsumer(EMAIL, null)
    }

    @Test
    fun `When customerEmail is set and network call fails then account status is Error`() = runSuspendTest {
        whenever(linkRepository.lookupConsumer(anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(Exception()))

        assertThat(accountManager(EMAIL).accountStatus.first()).isEqualTo(AccountStatus.Error)
    }

    @Test
    fun `When ConsumerSession contains consumerPublishableKey then key is updated`() {
        val accountManager = accountManager()

        assertThat(accountManager.consumerPublishableKey).isNull()

        accountManager.setAccountNullable(mockConsumerSession)

        assertThat(accountManager.consumerPublishableKey).isEqualTo(PUBLISHABLE_KEY)
    }

    @Test
    fun `When ConsumerSession is updated with the same email then consumerPublishableKey is kept`() {
        val accountManager = accountManager()

        accountManager.setAccountNullable(mockConsumerSession)

        assertThat(accountManager.consumerPublishableKey).isEqualTo(PUBLISHABLE_KEY)

        whenever(mockConsumerSession.publishableKey).thenReturn(null)
        accountManager.setAccountNullable(mockConsumerSession)

        assertThat(accountManager.consumerPublishableKey).isEqualTo(PUBLISHABLE_KEY)
    }

    @Test
    fun `When ConsumerSession is updated with different email then consumerPublishableKey is removed`() {
        val accountManager = accountManager()

        accountManager.setAccountNullable(mockConsumerSession)

        assertThat(accountManager.consumerPublishableKey).isEqualTo(PUBLISHABLE_KEY)

        whenever(mockConsumerSession.publishableKey).thenReturn(null)
        whenever(mockConsumerSession.emailAddress).thenReturn("different@email.com")
        accountManager.setAccountNullable(mockConsumerSession)

        assertThat(accountManager.consumerPublishableKey).isNull()
    }

    @Test
    fun `lookupConsumer sends analytics event when call fails`() = runSuspendTest {
        whenever(linkRepository.lookupConsumer(anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(Exception()))

        accountManager().lookupConsumer(EMAIL, false)

        verify(linkEventsReporter).onAccountLookupFailure()
    }

    @Test
    fun `signInWithUserInput sends correct parameters and starts session for existing user`() =
        runSuspendTest {
            val accountManager = accountManager()

            accountManager.signInWithUserInput(UserInput.SignIn(EMAIL))

            verify(linkRepository).lookupConsumer(eq(EMAIL), anyOrNull())
            assertThat(accountManager.linkAccount.value).isNotNull()
        }

    @Test
    fun `signInWithUserInput sends correct parameters and starts session for new user`() =
        runSuspendTest {
            val accountManager = accountManager()
            val phone = "phone"
            val country = "country"
            val name = "name"

            accountManager.signInWithUserInput(UserInput.SignUp(EMAIL, phone, country, name))

            verify(linkRepository).consumerSignUp(
                email = eq(EMAIL),
                phone = eq(phone),
                country = eq(country),
                name = eq(name),
                authSessionCookie = anyOrNull(),
                consentAction = eq(ConsumerSignUpConsentAction.Checkbox)
            )
            assertThat(accountManager.linkAccount.value).isNotNull()
        }

    @Test
    fun `signInWithUserInput for new user sends analytics event when call succeeds`() =
        runSuspendTest {
            accountManager().signInWithUserInput(
                UserInput.SignUp(
                    email = EMAIL,
                    phone = "phone",
                    country = "country",
                    name = "name"
                )
            )

            verify(linkEventsReporter).onSignupCompleted(true)
        }

    @Test
    fun `signInWithUserInput for new user sends analytics event when call fails`() =
        runSuspendTest {
            whenever(
                linkRepository.consumerSignUp(
                    email = anyOrNull(),
                    phone = anyOrNull(),
                    country = anyOrNull(),
                    name = anyOrNull(),
                    authSessionCookie = anyOrNull(),
                    consentAction = anyOrNull()
                )
            ).thenReturn(Result.failure(Exception()))

            accountManager().signInWithUserInput(
                UserInput.SignUp(
                    email = EMAIL,
                    phone = "phone",
                    country = "country",
                    name = "name"
                )
            )

            verify(linkEventsReporter).onSignupFailure(true)
        }

    @Test
    fun `createPaymentDetails for card does not retry on auth error`() =
        runSuspendTest {
            val accountManager = accountManager()
            accountManager.setAccountNullable(mockConsumerSession)

            whenever(
                linkRepository.createCardPaymentDetails(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            ).thenReturn(
                Result.failure(AuthenticationException(StripeError())),
                Result.success(mock())
            )

            accountManager.createCardPaymentDetails(mock(), "", mock())

            verify(linkRepository)
                .createCardPaymentDetails(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            verify(linkRepository, times(0)).lookupConsumer(anyOrNull(), anyOrNull())
        }

    private fun runSuspendTest(testBody: suspend TestScope.() -> Unit) = runTest {
        setupRepository()
        testBody()
    }

    private suspend fun setupRepository() {
        val consumerSessionLookup = mock<ConsumerSessionLookup>().apply {
            whenever(consumerSession).thenReturn(mockConsumerSession)
        }
        whenever(linkRepository.lookupConsumer(anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(consumerSessionLookup))
        whenever(
            linkRepository.consumerSignUp(
                email = anyOrNull(),
                phone = anyOrNull(),
                country = anyOrNull(),
                name = anyOrNull(),
                authSessionCookie = anyOrNull(),
                consentAction = any()
            )
        ).thenReturn(Result.success(mockConsumerSession))
    }

    private fun accountManager(
        customerEmail: String? = null,
        stripeIntent: StripeIntent = mock()
    ) = LinkAccountManager(
        config = LinkConfiguration(
            stripeIntent = stripeIntent,
            customerEmail = customerEmail,
            customerName = null,
            customerPhone = null,
            customerBillingCountryCode = null,
            merchantName = "Merchant",
            merchantCountryCode = "US",
            shippingValues = null,
        ),
        linkRepository,
        linkEventsReporter,
    )

    companion object {
        const val EMAIL = "email@stripe.com"
        const val CLIENT_SECRET = "client_secret"
        const val PUBLISHABLE_KEY = "publishable_key"
    }
}
