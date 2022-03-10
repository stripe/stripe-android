package com.stripe.android.link.ui.signup

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.signup.SignUpViewModel.Companion.LOOKUP_DEBOUNCE_MS
import com.stripe.android.model.ConsumerSession
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SignUpViewModelTest {
    private val defaultArgs = LinkActivityContract.Args(
        MERCHANT_NAME,
        CUSTOMER_EMAIL,
        LinkActivityContract.Args.InjectionParams(
            INJECTOR_KEY,
            setOf(PRODUCT_USAGE),
            true,
            PUBLISHABLE_KEY,
            STRIPE_ACCOUNT_ID
        )
    )
    private val linkAccountManager = mock<LinkAccountManager>()
    private val navigator = mock<Navigator>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When email is valid then lookup is triggered with delay`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(customerEmail = null))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "valid@email.com"))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            // Mock a delayed response so we stay in the loading state
            linkAccountManager.stub {
                onBlocking { lookupConsumer(any()) }.doSuspendableAnswer {
                    delay(100)
                    Result.success(mock())
                }
            }

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 1)

            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.VerifyingEmail)
        }

    @Test
    fun `When multiple valid emails entered quickly then lookup is triggered only for last one`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(customerEmail = null))
            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "first@email.com"))
            advanceTimeBy(LOOKUP_DEBOUNCE_MS / 2)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "second@email.com"))
            advanceTimeBy(LOOKUP_DEBOUNCE_MS / 2)

            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "third@email.com"))
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingEmail)

            // Mock a delayed response so we stay in the loading state
            linkAccountManager.stub {
                onBlocking { lookupConsumer(any()) }.doSuspendableAnswer {
                    delay(100)
                    Result.success(mock())
                }
            }

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 1)

            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.VerifyingEmail)

            val emailCaptor = argumentCaptor<String>()
            verify(linkAccountManager).lookupConsumer(emailCaptor.capture())

            assertThat(emailCaptor.allValues.size).isEqualTo(1)
            assertThat(emailCaptor.firstValue).isEqualTo("third@email.com")
        }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs)
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)

            verify(linkAccountManager, times(0)).lookupConsumer(any())
        }

    @Test
    fun `When signed up with unverified account then it navigates to Verification screen`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs)

            val linkAccount = LinkAccount(
                mockConsumerSessionWithVerificationSession(
                    ConsumerSession.VerificationSession.SessionType.Sms,
                    ConsumerSession.VerificationSession.SessionState.Started
                )
            )

            whenever(linkAccountManager.signUp(any(), any(), any()))
                .thenReturn(Result.success(linkAccount))

            viewModel.onSignUpClick("phone")

            verify(navigator).navigateTo(LinkScreen.Verification)
        }

    @Test
    fun `When signed up with verified account then it navigates to Wallet screen`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs)

            val linkAccount = LinkAccount(
                mockConsumerSessionWithVerificationSession(
                    ConsumerSession.VerificationSession.SessionType.Sms,
                    ConsumerSession.VerificationSession.SessionState.Verified
                )
            )

            whenever(linkAccountManager.signUp(any(), any(), any()))
                .thenReturn(Result.success(linkAccount))

            viewModel.onSignUpClick("phone")

            verify(navigator).navigateTo(LinkScreen.Wallet)
        }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val vmToBeReturned = mock<SignUpViewModel>()

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as SignUpViewModel.Factory
                factory.viewModel = vmToBeReturned
            }
        }
        WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)
        val factory = SignUpViewModel.Factory(
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(SignUpViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    private fun createViewModel(args: LinkActivityContract.Args = defaultArgs) = SignUpViewModel(
        args = args,
        linkAccountManager = linkAccountManager,
        logger = Logger.noop(),
        navigator = navigator
    )

    private fun mockConsumerSessionWithVerificationSession(
        type: ConsumerSession.VerificationSession.SessionType,
        state: ConsumerSession.VerificationSession.SessionState
    ): ConsumerSession {
        val verificationSession = mock<ConsumerSession.VerificationSession>()
        whenever(verificationSession.type).thenReturn(type)
        whenever(verificationSession.state).thenReturn(state)
        val verificationSessions = listOf(verificationSession)

        val consumerSession = mock<ConsumerSession>()
        whenever(consumerSession.verificationSessions).thenReturn(verificationSessions)
        whenever(consumerSession.clientSecret).thenReturn("secret")
        whenever(consumerSession.emailAddress).thenReturn("email")
        return consumerSession
    }

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
    }
}
