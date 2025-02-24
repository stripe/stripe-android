package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultLinkAccountManagerTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When cookie exists and network call fails then account status is Error`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.lookupConsumerResult = Result.failure(Exception())
        val accountManager = accountManager(TestFactory.EMAIL, linkRepository = linkRepository)
        assertThat(accountManager.accountStatus.first()).isEqualTo(AccountStatus.Error)
    }

    @Test
    fun `When customerEmail is set in arguments then it is looked up`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun lookupConsumer(email: String): Result<ConsumerSessionLookup> {
                if (email == TestFactory.EMAIL) callCount += 1
                return super.lookupConsumer(email)
            }
        }
        assertThat(
            accountManager(
                TestFactory.EMAIL,
                linkRepository = linkRepository
            ).accountStatus.first()
        ).isEqualTo(AccountStatus.Verified)

        assertThat(linkRepository.callCount).isEqualTo(1)
    }

    @Test
    fun `When customerEmail is set and network call fails then account status is Error`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.lookupConsumerResult = Result.failure(Exception())

        assertThat(
            accountManager(
                TestFactory.EMAIL,
                linkRepository = linkRepository
            ).accountStatus.first()
        ).isEqualTo(AccountStatus.Error)
    }

    @Test
    fun `When ConsumerSession contains consumerPublishableKey then key is updated`() = runTest {
        val linkRepository = FakeLinkRepository()
        val accountManager = accountManager(linkRepository = linkRepository)

        assertThat(accountManager.consumerPublishableKey).isNull()

        linkRepository.lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)

        accountManager.lookupConsumer(
            email = "email",
            startSession = true,
        )

        assertThat(accountManager.consumerPublishableKey).isEqualTo(TestFactory.PUBLISHABLE_KEY)
    }

    @Test
    fun `When ConsumerSession is updated with the same email then consumerPublishableKey is kept`() = runTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)

        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.lookupConsumer(
            email = "email",
            startSession = true,
        )

        assertThat(accountManager.consumerPublishableKey).isEqualTo(TestFactory.PUBLISHABLE_KEY)

        accountManager.setLinkAccountFromLookupResult(
            lookup = TestFactory.CONSUMER_SESSION_LOOKUP.copy(publishableKey = null),
            startSession = true,
        )

        assertThat(accountManager.consumerPublishableKey).isEqualTo(TestFactory.PUBLISHABLE_KEY)
    }

    @Test
    fun `When ConsumerSession is updated with different email then consumerPublishableKey is removed`() {
        val accountManager = accountManager()
        accountManager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP,
            startSession = true,
        )

        assertThat(accountManager.consumerPublishableKey).isEqualTo(TestFactory.PUBLISHABLE_KEY)

        accountManager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP.copy(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(emailAddress = "different@email.com"),
                publishableKey = null,
            ),
            startSession = true,
        )

        assertThat(accountManager.consumerPublishableKey).isNull()
    }

    @Test
    fun `lookupConsumer sends analytics event when call fails`() = runSuspendTest {
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun onAccountLookupFailure(error: Throwable) {
                callCount += 1
                super.onAccountLookupFailure(error)
            }
        }
        val linkRepository = FakeLinkRepository()
        linkRepository.lookupConsumerResult = Result.failure(Exception())

        accountManager(linkRepository = linkRepository, linkEventsReporter = linkEventsReporter)
            .lookupConsumer(
                TestFactory.EMAIL,
                false
            )

        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `signInWithUserInput sends correct parameters and starts session for existing user`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun lookupConsumer(email: String): Result<ConsumerSessionLookup> {
                if (email == TestFactory.EMAIL) callCount += 1
                return super.lookupConsumer(email)
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.signInWithUserInput(UserInput.SignIn(TestFactory.EMAIL))

        assertThat(linkRepository.callCount).isEqualTo(1)
        assertThat(accountManager.linkAccount.value).isNotNull()
    }

    @Test
    fun `signInWithUserInput sends correct parameters and starts session for new user`() = runSuspendTest {
        val phone = "phone"
        val country = "country"
        val name = "name"
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun consumerSignUp(
                actualEmail: String,
                actualPhone: String,
                actualCountry: String,
                actualName: String?,
                actualConsentAction: ConsumerSignUpConsentAction
            ): Result<ConsumerSessionSignup> {
                val userDetailsMatch = actualEmail == TestFactory.EMAIL &&
                    phone == actualPhone &&
                    country == actualCountry &&
                    name == actualName
                if (userDetailsMatch && actualConsentAction == ConsumerSignUpConsentAction.Checkbox) {
                    callCount += 1
                }
                return super.consumerSignUp(
                    actualEmail,
                    actualPhone,
                    actualCountry,
                    actualName,
                    actualConsentAction
                )
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.signInWithUserInput(
            UserInput.SignUp(
                email = TestFactory.EMAIL,
                phone = phone,
                country = country,
                name = name,
                consentAction = SignUpConsentAction.Checkbox
            )
        )

        assertThat(linkRepository.callCount).isEqualTo(1)
        assertThat(accountManager.linkAccount.value).isNotNull()
    }

    @Test
    fun `signInWithUserInput sends correct consumer action on 'Checkbox' consent action`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun consumerSignUp(
                email: String,
                phone: String,
                country: String,
                name: String?,
                consentAction: ConsumerSignUpConsentAction
            ): Result<ConsumerSessionSignup> {
                if (consentAction == ConsumerSignUpConsentAction.Checkbox) callCount += 1
                return super.consumerSignUp(email, phone, country, name, consentAction)
            }
        }
        accountManager(linkRepository = linkRepository).signInWithUserInput(
            createUserInputWithAction(
                SignUpConsentAction.Checkbox
            )
        )

        assertThat(linkRepository.callCount).isEqualTo(1)
    }

    @Test
    fun `signInWithUserInput sends correct consumer action on 'CheckboxWithPrefilledEmail' consent action`() =
        runSuspendTest {
            val linkRepository = object : FakeLinkRepository() {
                var callCount = 0
                override suspend fun consumerSignUp(
                    email: String,
                    phone: String,
                    country: String,
                    name: String?,
                    consentAction: ConsumerSignUpConsentAction
                ): Result<ConsumerSessionSignup> {
                    if (consentAction == ConsumerSignUpConsentAction.CheckboxWithPrefilledEmail) {
                        callCount += 1
                    }
                    return super.consumerSignUp(email, phone, country, name, consentAction)
                }
            }
            accountManager(linkRepository = linkRepository).signInWithUserInput(
                createUserInputWithAction(
                    SignUpConsentAction.CheckboxWithPrefilledEmail
                )
            )

            assertThat(linkRepository.callCount).isEqualTo(1)
        }

    @Test
    fun `signInWithUserInput sends correct consumer action on 'CheckboxWithPrefilledEmailAndPhone' consent action`() =
        runSuspendTest {
            val linkRepository = object : FakeLinkRepository() {
                var callCount = 0
                override suspend fun consumerSignUp(
                    email: String,
                    phone: String,
                    country: String,
                    name: String?,
                    consentAction: ConsumerSignUpConsentAction
                ): Result<ConsumerSessionSignup> {
                    if (consentAction == ConsumerSignUpConsentAction.CheckboxWithPrefilledEmailAndPhone) {
                        callCount += 1
                    }
                    return super.consumerSignUp(email, phone, country, name, consentAction)
                }
            }
            accountManager(linkRepository = linkRepository).signInWithUserInput(
                createUserInputWithAction(
                    SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
                )
            )

            assertThat(linkRepository.callCount).isEqualTo(1)
        }

    @Test
    fun `signInWithUserInput sends correct consumer action on 'Implied' consent action`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun consumerSignUp(
                email: String,
                phone: String,
                country: String,
                name: String?,
                consentAction: ConsumerSignUpConsentAction
            ): Result<ConsumerSessionSignup> {
                if (consentAction == ConsumerSignUpConsentAction.Implied) {
                    callCount += 1
                }
                return super.consumerSignUp(email, phone, country, name, consentAction)
            }
        }
        accountManager(linkRepository = linkRepository).signInWithUserInput(
            createUserInputWithAction(
                SignUpConsentAction.Implied
            )
        )

        assertThat(linkRepository.callCount).isEqualTo(1)
    }

    @Test
    fun `signInWithUserInput sends correct consumer action on 'ImpliedWithPrefilledEmail' consent action`() =
        runSuspendTest {
            val linkRepository = object : FakeLinkRepository() {
                val consentActions = arrayListOf<ConsumerSignUpConsentAction>()
                override suspend fun consumerSignUp(
                    email: String,
                    phone: String,
                    country: String,
                    name: String?,
                    consentAction: ConsumerSignUpConsentAction
                ): Result<ConsumerSessionSignup> {
                    consentActions.add(consentAction)
                    return super.consumerSignUp(email, phone, country, name, consentAction)
                }
            }
            accountManager(linkRepository = linkRepository).signInWithUserInput(
                createUserInputWithAction(
                    SignUpConsentAction.ImpliedWithPrefilledEmail
                )
            )

            assertThat(linkRepository.consentActions)
                .isEqualTo(listOf(ConsumerSignUpConsentAction.ImpliedWithPrefilledEmail))
        }

    @Test
    fun `signInWithUserInput for new user sends analytics event when call succeeds`() = runSuspendTest {
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun onSignupCompleted(isInline: Boolean) {
                if (isInline) callCount += 1
                super.onSignupCompleted(isInline)
            }
        }
        accountManager(linkEventsReporter = linkEventsReporter).signInWithUserInput(
            UserInput.SignUp(
                email = TestFactory.EMAIL,
                phone = "phone",
                country = "country",
                name = "name",
                consentAction = SignUpConsentAction.Checkbox
            )
        )

        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `signInWithUserInput for new user fails when user is logged in`() = runSuspendTest {
        val manager = accountManager()

        manager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP,
            startSession = true,
        )

        val result = manager.signInWithUserInput(
            UserInput.SignUp(
                email = TestFactory.EMAIL,
                phone = "phone",
                country = "country",
                name = "name",
                consentAction = SignUpConsentAction.Checkbox
            )
        )

        assertThat(result.exceptionOrNull()).isEqualTo(
            AlreadyLoggedInLinkException(
                email = TestFactory.EMAIL,
                accountStatus = AccountStatus.Verified
            )
        )
    }

    @Test
    fun `signInWithUserInput for new user sends event when fails when user is logged in`() = runSuspendTest {
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) {
                if (state == LinkEventsReporter.SessionState.Verified) callCount += 1
                super.onInvalidSessionState(state)
            }
        }
        val manager = accountManager(linkEventsReporter = linkEventsReporter)

        manager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP,
            startSession = true,
        )

        manager.signInWithUserInput(
            UserInput.SignUp(
                email = TestFactory.EMAIL,
                phone = "phone",
                country = "country",
                name = "name",
                consentAction = SignUpConsentAction.Checkbox
            )
        )

        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `signInWithUserInput for new user sends analytics event when call fails`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.consumerSignUpResult = Result.failure(Exception())

        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun onSignupFailure(isInline: Boolean, error: Throwable) {
                if (isInline) callCount += 1
                super.onSignupFailure(isInline, error)
            }
        }

        accountManager(linkRepository = linkRepository, linkEventsReporter = linkEventsReporter)
            .signInWithUserInput(
                UserInput.SignUp(
                    email = TestFactory.EMAIL,
                    phone = "phone",
                    country = "country",
                    name = "name",
                    consentAction = SignUpConsentAction.Checkbox
                )
            )

        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `createPaymentDetails for card does not retry on auth error`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var result = listOf(
                Result.failure(AuthenticationException(StripeError())),
                Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
            )
            var callCount = 0
            override suspend fun createCardPaymentDetails(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                userEmail: String,
                stripeIntent: StripeIntent,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?,
                active: Boolean
            ): Result<LinkPaymentDetails.New> {
                val details = result.first()
                if (result.size > 1) {
                    result = result.subList(1, result.size)
                }
                return details
            }

            override suspend fun lookupConsumer(email: String): Result<ConsumerSessionLookup> {
                callCount += 1
                return super.lookupConsumer(email)
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP,
            startSession = true,
        )

        accountManager.createCardPaymentDetails(TestFactory.PAYMENT_METHOD_CREATE_PARAMS)

        assertThat(linkRepository.result.size).isEqualTo(1)
        assertThat(linkRepository.callCount).isEqualTo(0)
    }

    @Test
    fun `createCardPaymentDetails makes correct calls in passthrough mode`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var createCardPaymentDetailsCallCount = 0
            var shareCardPaymentDetailsCallCount = 0
            override suspend fun createCardPaymentDetails(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                userEmail: String,
                stripeIntent: StripeIntent,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?,
                active: Boolean
            ): Result<LinkPaymentDetails.New> {
                createCardPaymentDetailsCallCount += 1
                return Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
            }

            override suspend fun shareCardPaymentDetails(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                id: String,
                last4: String,
                consumerSessionClientSecret: String
            ): Result<LinkPaymentDetails.New> {
                val paymentDetailsMatch = paymentMethodCreateParams == TestFactory.PAYMENT_METHOD_CREATE_PARAMS &&
                    id == TestFactory.LINK_NEW_PAYMENT_DETAILS.paymentDetails.id &&
                    last4 == TestFactory.LINK_NEW_PAYMENT_DETAILS.paymentDetails.last4
                if (paymentDetailsMatch && consumerSessionClientSecret == TestFactory.CLIENT_SECRET) {
                    shareCardPaymentDetailsCallCount += 1
                }
                return super.shareCardPaymentDetails(
                    paymentMethodCreateParams,
                    id,
                    last4,
                    consumerSessionClientSecret
                )
            }
        }
        val accountManager = accountManager(passthroughModeEnabled = true, linkRepository = linkRepository)

        accountManager.setLinkAccountFromLookupResult(
            TestFactory.CONSUMER_SESSION_LOOKUP,
            startSession = true,
        )

        val result = accountManager.createCardPaymentDetails(TestFactory.PAYMENT_METHOD_CREATE_PARAMS)

        assertThat(result.isSuccess).isTrue()
        val linkPaymentDetails = result.getOrThrow()
        assertThat(linkPaymentDetails.paymentDetails.id)
            .isEqualTo(TestFactory.LINK_NEW_PAYMENT_DETAILS.paymentDetails.id)

        assertThat(linkRepository.createCardPaymentDetailsCallCount).isEqualTo(1)
        assertThat(linkRepository.shareCardPaymentDetailsCallCount).isEqualTo(1)
        assertThat(accountManager.linkAccount.value).isNotNull()
    }

    @Test
    fun `lookupConsumer does not start session when startSession is false`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun startVerification(
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerSession> {
                callCount += 1
                return super.startVerification(consumerSessionClientSecret, consumerPublishableKey)
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.lookupConsumer(TestFactory.EMAIL, false)

        assertThat(linkRepository.callCount).isEqualTo(0)
        assertThat(accountManager.linkAccount.value).isNull()
    }

    @Test
    fun `startVerification updates account`() = runSuspendTest {
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun on2FAStart() {
                callCount += 1
            }
        }
        val accountManager = accountManager(linkEventsReporter = linkEventsReporter)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, null)

        accountManager.startVerification()

        assertThat(accountManager.linkAccount.value).isNotNull()
        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `startVerification uses consumerPublishableKey`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var consumerPublishableKey: String? = null
            var callCount = 0
            override suspend fun startVerification(
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerSession> {
                callCount += 1
                this.consumerPublishableKey = consumerPublishableKey
                return super.startVerification(consumerSessionClientSecret, consumerPublishableKey)
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        accountManager.startVerification()

        assertThat(linkRepository.callCount).isEqualTo(1)
        assertThat(linkRepository.consumerPublishableKey).isEqualTo(TestFactory.PUBLISHABLE_KEY)
    }

    @Test
    fun `startVerification sends analytics event when call fails`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.startVerificationResult = Result.failure(Exception())

        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun on2FAStartFailure() {
                callCount += 1
                super.on2FAStartFailure()
            }
        }

        val accountManager = accountManager(linkRepository = linkRepository, linkEventsReporter = linkEventsReporter)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, null)
        accountManager.startVerification()

        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `confirmVerification returns error when link account is null`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        val accountManager = accountManager(
            linkRepository = linkRepository,
        )
        accountManager.setAccountNullable(null, null)

        val result = accountManager.confirmVerification("123")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("no link account found")
    }

    @Test
    fun `confirmVerification returns success result when verification succeeds`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun confirmVerification(
                verificationCode: String,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerSession> {
                callCount += 1
                return super.confirmVerification(verificationCode, consumerSessionClientSecret, consumerPublishableKey)
            }
        }
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun on2FAComplete() {
                callCount += 1
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository, linkEventsReporter = linkEventsReporter)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, null)

        linkRepository.confirmVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)

        val result = accountManager.confirmVerification("123")

        assertThat(linkRepository.callCount).isEqualTo(1)
        assertThat(result.isSuccess).isTrue()
        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `confirmVerification returns failure result when verification fails`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun confirmVerification(
                verificationCode: String,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerSession> {
                callCount += 1
                return Result.failure(error)
            }
        }
        val linkEventsReporter = object : AccountManagerEventsReporter() {
            var callCount = 0
            override fun on2FAFailure() {
                callCount += 1
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository, linkEventsReporter = linkEventsReporter)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, null)

        val result = accountManager.confirmVerification("123")

        assertThat(linkRepository.callCount).isEqualTo(1)
        assertThat(result).isEqualTo(Result.failure<LinkAccount>(error))
        assertThat(linkEventsReporter.callCount).isEqualTo(1)
    }

    @Test
    fun `listPaymentDetails returns error when repository call fails`() = runSuspendTest {
        val error = AuthenticationException(StripeError())
        val linkRepository = object : FakeLinkRepository() {
            var paymentMethodTypes: Set<String>? = null
            override suspend fun listPaymentDetails(
                paymentMethodTypes: Set<String>,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerPaymentDetails> {
                this.paymentMethodTypes = paymentMethodTypes
                return Result.failure(error)
            }
        }

        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        val result = accountManager.listPaymentDetails(setOf("card"))

        assertThat(result.exceptionOrNull()).isEqualTo(error)
        assertThat(linkRepository.paymentMethodTypes).isEqualTo(setOf("card"))
    }

    @Test
    fun `listPaymentDetails returns success when repository call succeeds`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var paymentMethodTypes: Set<String>? = null
            override suspend fun listPaymentDetails(
                paymentMethodTypes: Set<String>,
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerPaymentDetails> {
                this.paymentMethodTypes = paymentMethodTypes
                return Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
            }
        }

        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        val result = accountManager.listPaymentDetails(setOf("card"))

        assertThat(result.getOrNull()).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS)
        assertThat(linkRepository.paymentMethodTypes).isEqualTo(setOf("card"))
    }

    @Test
    fun `deletePaymentDetails returns error when repository call fails`() = runSuspendTest {
        val error = AuthenticationException(StripeError())
        val linkRepository = FakeLinkRepository()

        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        linkRepository.deletePaymentDetailsResult = Result.failure(error)

        val result = accountManager.deletePaymentDetails("id")

        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    @Test
    fun `deletePaymentDetails returns success when repository call succeeds`() = runSuspendTest {
        val accountManager = accountManager()
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        val result = accountManager.deletePaymentDetails("id")

        assertThat(result.getOrNull()).isEqualTo(Unit)
    }

    @Test
    fun `updatePaymentDetails returns error when repository call fails`() = runSuspendTest {
        val error = AuthenticationException(StripeError())
        val linkRepository = FakeLinkRepository()

        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        linkRepository.updatePaymentDetailsResult = Result.failure(error)

        val result = accountManager.updatePaymentDetails(
            updateParams = ConsumerPaymentDetailsUpdateParams("")
        )

        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    @Test
    fun `updatePaymentDetails returns success when repository call succeeds`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()

        val accountManager = accountManager(linkRepository = linkRepository)
        accountManager.setAccountNullable(TestFactory.CONSUMER_SESSION, TestFactory.PUBLISHABLE_KEY)

        linkRepository.updatePaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)

        val result = accountManager.updatePaymentDetails(ConsumerPaymentDetailsUpdateParams(""))

        assertThat(result.getOrNull()).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS)
    }

    @Test
    fun `mobileLookup returns link account on success`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        val accountManager = accountManager(
            linkRepository = linkRepository
        )

        val result = accountManager.mobileLookupConsumer(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            startSession = false
        )

        val call = linkRepository.awaitMobileLookup()
        assertThat(call.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(call.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(call.emailSource).isEqualTo(TestFactory.EMAIL_SOURCE)
        assertThat(call.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(call.sessionId).isEqualTo(TestFactory.LINK_CONFIGURATION.elementsSessionId)

        assertThat(result.getOrNull()?.email).isEqualTo(TestFactory.LINK_ACCOUNT.email)
        assertThat(accountManager.linkAccount.value).isNull()

        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `mobileLookup returns link account and starts session when startSession is true`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()

        val accountManager = accountManager(
            linkRepository = linkRepository
        )

        val result = accountManager.mobileLookupConsumer(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            startSession = true
        )

        linkRepository.awaitMobileLookup()

        assertThat(result.getOrNull()?.email).isEqualTo(TestFactory.LINK_ACCOUNT.email)
        assertThat(accountManager.linkAccount.value?.email).isEqualTo(TestFactory.LINK_ACCOUNT.email)

        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `mobileLookup returns error and logs event on failure`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = FakeLinkRepository()
        val linkEventsReporter = AccountManagerEventsReporter()

        linkRepository.mobileLookupConsumerResult = Result.failure(error)

        val accountManager = accountManager(
            linkRepository = linkRepository,
            linkEventsReporter = linkEventsReporter
        )

        val result = accountManager.mobileLookupConsumer(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            startSession = true
        )

        linkRepository.awaitMobileLookup()
        val analyticsCall = linkEventsReporter.awaitLookupFailureCall()

        assertThat(result.exceptionOrNull()).isEqualTo(error)
        assertThat(accountManager.linkAccount.value).isNull()
        assertThat(analyticsCall).isEqualTo(error)

        linkRepository.ensureAllEventsConsumed()
        linkEventsReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `mobileSignUp returns link account on success`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()

        val accountManager = accountManager(
            linkRepository = linkRepository
        )

        val result = accountManager.mobileSignUp(
            email = TestFactory.CUSTOMER_EMAIL,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            country = TestFactory.COUNTRY,
            phone = TestFactory.CUSTOMER_PHONE,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        val call = linkRepository.awaitMobileSignup()
        assertThat(call.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(call.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(call.phoneNumber).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(call.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(call.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(call.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(call.consentAction).isEqualTo(ConsumerSignUpConsentAction.Implied)

        assertThat(result.getOrNull()?.email).isEqualTo(TestFactory.LINK_ACCOUNT.email)
        assertThat(accountManager.linkAccount.value?.email).isEqualTo(TestFactory.LINK_ACCOUNT.email)

        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `mobileSignUp returns error on failure`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = FakeLinkRepository()
        linkRepository.mobileConsumerSignUpResult = Result.failure(error)

        val accountManager = accountManager(
            linkRepository = linkRepository
        )

        val result = accountManager.mobileSignUp(
            email = TestFactory.CUSTOMER_EMAIL,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            country = TestFactory.COUNTRY,
            phone = TestFactory.CUSTOMER_PHONE,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        linkRepository.awaitMobileSignup()

        assertThat(result.exceptionOrNull()).isEqualTo(error)
        assertThat(accountManager.linkAccount.value).isNull()

        linkRepository.ensureAllEventsConsumed()
    }

    private fun runSuspendTest(testBody: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        testBody()
    }

    private fun accountManager(
        customerEmail: String? = null,
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
        passthroughModeEnabled: Boolean = false,
        linkRepository: LinkRepository = FakeLinkRepository(),
        linkEventsReporter: LinkEventsReporter = AccountManagerEventsReporter()
    ): DefaultLinkAccountManager {
        val customerInfo = TestFactory.LINK_CONFIGURATION.customerInfo.copy(
            email = customerEmail,
        )
        return DefaultLinkAccountManager(
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            config = TestFactory.LINK_CONFIGURATION.copy(
                stripeIntent = stripeIntent,
                passthroughModeEnabled = passthroughModeEnabled,
                customerInfo = customerInfo
            ),
            linkRepository,
            linkEventsReporter,
            errorReporter = FakeErrorReporter()
        )
    }

    private fun createUserInputWithAction(consentAction: SignUpConsentAction): UserInput.SignUp {
        return UserInput.SignUp(
            email = TestFactory.EMAIL,
            phone = "phone",
            country = "country",
            name = "name",
            consentAction = consentAction
        )
    }
}

private open class AccountManagerEventsReporter : FakeLinkEventsReporter() {
    private val lookupFailureTurbine = Turbine<Throwable>()
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) = Unit
    override fun onSignupCompleted(isInline: Boolean) = Unit
    override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
    override fun onAccountLookupFailure(error: Throwable) {
        lookupFailureTurbine.add(error)
    }

    override fun on2FAStartFailure() = Unit
    override fun on2FAStart() = Unit
    override fun on2FAComplete() = Unit
    override fun on2FAFailure() = Unit

    suspend fun awaitLookupFailureCall(): Throwable {
        return lookupFailureTurbine.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        lookupFailureTurbine.ensureAllEventsConsumed()
    }
}
