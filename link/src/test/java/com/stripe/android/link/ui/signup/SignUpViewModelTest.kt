package com.stripe.android.link.ui.signup

import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
internal class SignUpViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val config = LinkConfiguration(
        stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        merchantCountryCode = "",
        customerInfo = LinkConfiguration.CustomerInfo(
            name = CUSTOMER_NAME,
            email = CUSTOMER_EMAIL,
            phone = CUSTOMER_PHONE,
            billingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE
        ),
        shippingValues = null,
        flags = emptyMap(),
        cardBrandChoice = null,
        passthroughModeEnabled = false
    )
    private val defaultArgs = LinkActivityContract.Args(
        configuration = config,
    )

    private val navController: NavHostController = mock()
    private val linkAccountManager: LinkAccountManager = mock()
    private val linkEventsReporter: LinkEventsReporter = mock()
    private val logger = mock<Logger>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init sends analytics event`() = runTest(dispatcher) {
        hasUserLoggedOut()

        createViewModel()

        verify(linkEventsReporter).onSignupFlowPresented()
    }

    @Test
    fun `form should not be prefilled when user is not logged out`() = runTest(dispatcher) {
        hasUserLoggedOut(yes = true)

        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL
        )

        assertThat(viewModel.contentState?.phoneNumberController?.fieldValue?.value).isEqualTo("")
        assertThat(viewModel.contentState?.emailController?.fieldValue?.value).isEqualTo("")
        assertThat(viewModel.contentState?.nameController?.fieldValue?.value).isEqualTo("")
    }

    @Test
    fun `form should be prefilled when user is logged out`() = runTest(dispatcher) {
        hasUserLoggedOut(yes = false)

        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL
        )

        assertThat(viewModel.contentState?.phoneNumberController?.fieldValue?.value).isEqualTo(CUSTOMER_PHONE)
        assertThat(viewModel.contentState?.emailController?.fieldValue?.value).isEqualTo(CUSTOMER_EMAIL)
        assertThat(viewModel.contentState?.nameController?.fieldValue?.value).isEqualTo(CUSTOMER_NAME)
    }

    @Test
    fun `When email is valid then lookup is triggered with delay`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel(prefilledEmail = null)

        assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        viewModel.contentState?.emailController?.onRawValueChange("valid@email.com")
        assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        // Mock a delayed response so we stay in the loading state
        linkAccountManager.stub {
            onBlocking { lookupConsumer(any(), any()) }.doSuspendableAnswer {
                delay(100)
                Result.success(mock())
            }
        }

        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.contentState?.emailController?.fieldValue?.value).isEqualTo("valid@email.com")
        assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.VerifyingEmail)
    }

    @Test
    fun `When multiple valid emails entered quickly then lookup is triggered only for last one`() =
        runTest(dispatcher) {
            hasUserLoggedOut()

            val viewModel = createViewModel(prefilledEmail = null)

            viewModel.contentState?.emailController?.onRawValueChange("first@email.com")
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE / 2)

            viewModel.contentState?.emailController?.onRawValueChange("second@email.com")
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE / 2)

            viewModel.contentState?.emailController?.onRawValueChange("third@email.com")
            assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

            // Mock a delayed response so we stay in the loading state
            linkAccountManager.stub {
                onBlocking { lookupConsumer(any(), any()) }.doSuspendableAnswer {
                    delay(100)
                    Result.success(mock())
                }
            }

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.VerifyingEmail)

            val emailCaptor = argumentCaptor<String>()
            verify(linkAccountManager).lookupConsumer(emailCaptor.capture(), any())

            assertThat(emailCaptor.allValues.size).isEqualTo(1)
            assertThat(emailCaptor.firstValue).isEqualTo("third@email.com")
        }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL)

        assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
        verify(linkAccountManager, times(0)).lookupConsumer(any(), any())
    }

    @Test
    fun `When lookupConsumer succeeds for new account then analytics event is sent`() = runTest(dispatcher) {
        hasUserLoggedOut()
        whenever(linkAccountManager.lookupConsumer(any(), any()))
            .thenReturn(Result.success(null))

        val viewModel = createViewModel()

        viewModel.contentState?.emailController?.onRawValueChange("valid@email.com")
        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        verify(linkEventsReporter).onSignupStarted()
    }

    @Test
    fun `When lookupConsumer fails then an error message is shown`() = runTest(dispatcher) {
        hasUserLoggedOut()
        val errorMessage = "Error message"
        whenever(linkAccountManager.lookupConsumer(any(), any()))
            .thenReturn(Result.failure(RuntimeException(errorMessage)))

        val viewModel = createViewModel()

        viewModel.contentState?.emailController?.onRawValueChange(VALID_EMAIL)
        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.contentState?.errorMessage).isEqualTo(errorMessage.resolvableString)
    }

    @Test
    fun `signUp sends correct ConsumerSignUpConsentAction`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel()

        viewModel.performValidSignup()

        verify(linkAccountManager).signUp(
            any(),
            any(),
            any(),
            anyOrNull(),
            eq(SignUpConsentAction.Implied)
        )
    }

    @Test
    fun `When signUp fails then an error message is shown`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val errorMessage = "Error message"
        whenever(linkAccountManager.signUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(Result.failure(RuntimeException(errorMessage)))

        val viewModel = createViewModel()
        viewModel.performValidSignup()

        assertThat(viewModel.contentState?.errorMessage).isEqualTo(errorMessage.resolvableString)
    }

    @Test
    fun `When signed up with unverified account then it navigates to Verification screen`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel()

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Started
            )
        )
        whenever(linkAccountManager.signUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(Result.success(linkAccount))

        viewModel.performValidSignup()

        verify(navController).navigate(LinkScreen.Verification.route)
        assertThat(viewModel.contentState?.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
    }

    @Test
    fun `When signed up with verified account then it navigates to Wallet screen`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel()

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Verified
            )
        )
        whenever(linkAccountManager.signUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(Result.success(linkAccount))

        viewModel.performValidSignup()

        verify(navController).popBackStack(LinkScreen.Wallet.route, inclusive = false)
    }

    @Test
    fun `When signup succeeds then analytics event is sent`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel()

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Verified
            )
        )

        whenever(linkAccountManager.signUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(Result.success(linkAccount))

        viewModel.performValidSignup()

        verify(linkEventsReporter).onSignupCompleted()
    }

    @Test
    fun `When signup fails then analytics event is sent`() = runTest(dispatcher) {
        hasUserLoggedOut()
        val error = Exception()

        val viewModel = createViewModel()

        whenever(linkAccountManager.signUp(any(), any(), any(), anyOrNull(), any()))
            .thenReturn(Result.failure(error))

        viewModel.performValidSignup()

        verify(linkEventsReporter).onSignupFailure(eq(false), eq(error))
    }

    @Test
    fun `Doesn't require name for US consumers`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel(
            prefilledEmail = null,
            countryCode = CountryCode.US
        )

        assertThat(viewModel.contentState?.signUpEnabled).isFalse()

        viewModel.contentState?.emailController?.onRawValueChange("me@myself.com")
        viewModel.contentState?.phoneNumberController?.onRawValueChange("1234567890")
        assertThat(viewModel.contentState?.signUpEnabled).isTrue()
    }

    @Test
    fun `Requires name for non-US consumers`() = runTest(dispatcher) {
        hasUserLoggedOut()

        val viewModel = createViewModel(
            prefilledEmail = null,
            countryCode = CountryCode.CA
        )

        assertThat(viewModel.contentState?.signUpEnabled).isFalse()

        viewModel.contentState?.emailController?.onRawValueChange("me@myself.com")
        viewModel.contentState?.phoneNumberController?.onRawValueChange("1234567890")
        viewModel.contentState?.nameController?.onRawValueChange("")
        assertThat(viewModel.contentState?.signUpEnabled).isFalse()

        viewModel.contentState?.nameController?.onRawValueChange("Someone from Canada")
        assertThat(viewModel.contentState?.signUpEnabled).isTrue()
    }

    @Test
    fun `Prefilled values are handled correctly`() = runTest(dispatcher) {
        hasUserLoggedOut(yes = false)

        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL,
            countryCode = CountryCode.US
        )

        assertThat(viewModel.contentState?.signUpEnabled).isTrue()
    }

    private suspend fun hasUserLoggedOut(yes: Boolean = true) {
        whenever(linkAccountManager.hasUserLoggedOut(anyOrNull())).thenAnswer { yes }
    }

    private fun SignUpViewModel.performValidSignup() {
        contentState?.emailController?.onRawValueChange("email@valid.co")
        contentState?.phoneNumberController?.onRawValueChange("1234567890")
        onSignUpClick()
    }

    private fun createViewModel(
        prefilledEmail: String? = null,
        args: LinkActivityContract.Args = defaultArgs,
        countryCode: CountryCode = CountryCode.US
    ): SignUpViewModel {
        return SignUpViewModel(
            args = args.copy(
                configuration = args.configuration.copy(
                    customerInfo = args.configuration.customerInfo.copy(
                        email = prefilledEmail,
                    ),
                    stripeIntent = when (val intent = args.configuration.stripeIntent) {
                        is PaymentIntent -> intent.copy(countryCode = countryCode.value)
                        is SetupIntent -> intent.copy(countryCode = countryCode.value)
                    }
                )
            ),
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
        ).apply {
            this.navController = this@SignUpViewModelTest.navController
        }
    }

    private fun mockConsumerSessionWithVerificationSession(
        type: ConsumerSession.VerificationSession.SessionType,
        state: ConsumerSession.VerificationSession.SessionState
    ): ConsumerSession {
        return ConsumerSession(
            emailAddress = "",
            redactedPhoneNumber = "",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = type,
                    state = state
                )
            ),
            redactedFormattedPhoneNumber = ""
        )
    }

    private val SignUpViewModel.contentState: SignUpScreenState.Content?
        get() = state.value as? SignUpScreenState.Content

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
        const val CUSTOMER_PHONE = "1234567890"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "US"
        const val CUSTOMER_NAME = "Customer"
        const val VALID_EMAIL = "email@valid.co"
    }
}
