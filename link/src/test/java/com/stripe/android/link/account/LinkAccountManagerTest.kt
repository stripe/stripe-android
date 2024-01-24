package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
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
        whenever(emailAddress).thenReturn(EMAIL)
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

        verify(linkEventsReporter).onAccountLookupFailure(any<Exception>())
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
    fun `signInWithUserInput for new user fails when user is logged in`() =
        runSuspendTest {
            val manager = accountManager()

            manager.setAccountNullable(mockConsumerSession)

            val result = manager.signInWithUserInput(
                UserInput.SignUp(
                    email = EMAIL,
                    phone = "phone",
                    country = "country",
                    name = "name"
                )
            )

            assertThat(result.exceptionOrNull()).isEqualTo(
                AlreadyLoggedInLinkException(
                    email = EMAIL,
                    accountStatus = AccountStatus.Verified
                )
            )
        }

    @Test
    fun `signInWithUserInput for new user sends event when fails when user is logged in`() =
        runSuspendTest {
            val manager = accountManager()

            manager.setAccountNullable(mockConsumerSession)

            manager.signInWithUserInput(
                UserInput.SignUp(
                    email = EMAIL,
                    phone = "phone",
                    country = "country",
                    name = "name"
                )
            )

            verify(linkEventsReporter).onInvalidSessionState(LinkEventsReporter.SessionState.Verified)
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

            verify(linkEventsReporter).onSignupFailure(eq(true), any<Exception>())
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
                    anyOrNull(),
                    anyOrNull(),
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
                    anyOrNull(),
                    anyOrNull(),
                )
            verify(linkRepository, times(0)).lookupConsumer(anyOrNull(), anyOrNull())
        }

    @Test
    fun `createCardPaymentDetails makes correct calls in passthrough mode`() =
        runSuspendTest {
            val accountManager = accountManager(passthroughModeEnabled = true)
            accountManager.setAccountNullable(mockConsumerSession)

            val paymentDetails = mock<ConsumerPaymentDetails.PaymentDetails>().apply {
                whenever(id).thenReturn("csmrpd*AYq4D_sXdAAAAOQ0")
            }
            whenever(
                linkRepository.createCardPaymentDetails(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ).thenReturn(
                Result.success(LinkPaymentDetails.New(paymentDetails, mock(), mock()))
            )

            val paymentMethodCreateParams = PaymentMethodCreateParams.createCard(
                CardParams(
                    number = "4242424242424242",
                    expMonth = 1,
                    expYear = 27,
                    cvc = "123",
                )
            )
            val result = accountManager.createCardPaymentDetails(paymentMethodCreateParams)
            assertThat(result.isSuccess).isTrue()
            val linkPaymentDetails = result.getOrThrow()
            assertThat(linkPaymentDetails.paymentDetails.id).isEqualTo(PAYMENT_METHOD_ID)

            verify(linkRepository)
                .createCardPaymentDetails(
                    paymentMethodCreateParams = anyOrNull(),
                    userEmail = anyOrNull(),
                    stripeIntent = anyOrNull(),
                    consumerSessionClientSecret = anyOrNull(),
                    consumerPublishableKey = anyOrNull(),
                    active = anyOrNull(),
                )
            verify(linkRepository).shareCardPaymentDetails(
                paymentMethodCreateParams = eq(paymentMethodCreateParams),
                id = eq("csmrpd*AYq4D_sXdAAAAOQ0"),
                last4 = eq("4242"),
                consumerSessionClientSecret = eq(CLIENT_SECRET),
            )
            assertThat(accountManager.linkAccount.value).isNotNull()
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
        whenever(
            linkRepository.shareCardPaymentDetails(
                paymentMethodCreateParams = anyOrNull(),
                id = anyOrNull(),
                last4 = anyOrNull(),
                consumerSessionClientSecret = anyOrNull(),
            )
        ).thenReturn(
            Result.success(
                LinkPaymentDetails.Saved(
                    paymentDetails = ConsumerPaymentDetails.Passthrough(
                        id = PAYMENT_METHOD_ID,
                        last4 = "1234",
                    ),
                    paymentMethodCreateParams = PaymentMethodCreateParams.createLink(
                        paymentDetailsId = PAYMENT_METHOD_ID,
                        consumerSessionClientSecret = CLIENT_SECRET,
                    ),
                )
            )
        )
    }

    private fun accountManager(
        customerEmail: String? = null,
        stripeIntent: StripeIntent = mock(),
        passthroughModeEnabled: Boolean = false,
    ) = LinkAccountManager(
        config = LinkConfiguration(
            stripeIntent = stripeIntent,
            customerInfo = LinkConfiguration.CustomerInfo(
                name = null,
                email = customerEmail,
                phone = null,
                billingCountryCode = null,
                shouldPrefill = true,
            ),
            merchantName = "Merchant",
            merchantCountryCode = "US",
            shippingValues = null,
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            passthroughModeEnabled = passthroughModeEnabled,
        ),
        linkRepository,
        linkEventsReporter,
    )

    private companion object {
        const val EMAIL = "email@stripe.com"
        const val CLIENT_SECRET = "client_secret"
        const val PUBLISHABLE_KEY = "publishable_key"
        const val PAYMENT_METHOD_ID = "pm_1NsnWALu5o3P18Zp36Q7YfWW"
    }
}
