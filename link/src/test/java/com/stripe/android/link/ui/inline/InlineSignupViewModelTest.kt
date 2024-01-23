package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
class InlineSignupViewModelTest {
    private val linkAccountManager = mock<LinkAccountManager>()
    private val linkEventsReporter = mock<LinkEventsReporter>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When email and phone are provided it should prefill all values`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = InlineSignupViewModel(
                config = LinkConfiguration(
                    stripeIntent = mockStripeIntent(),
                    merchantName = MERCHANT_NAME,
                    merchantCountryCode = "US",
                    customerInfo = LinkConfiguration.CustomerInfo(
                        email = CUSTOMER_EMAIL,
                        phone = CUSTOMER_PHONE,
                        name = CUSTOMER_NAME,
                        billingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE,
                        shouldPrefill = true,
                    ),
                    shippingValues = null,
                    signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    passthroughModeEnabled = false,
                ),
                linkAccountManager = linkAccountManager,
                linkEventsReporter = linkEventsReporter,
                logger = Logger.noop()
            )

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            viewModel.toggleExpanded()
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 1) // Trigger lookup by waiting for delay.
            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingPhoneOrName)
            assertThat(viewModel.emailController.fieldValue.first()).isEqualTo(CUSTOMER_EMAIL)
            assertThat(viewModel.phoneController.fieldValue.first()).isEqualTo(CUSTOMER_PHONE)

            verify(linkAccountManager).lookupConsumer(any(), any())
        }

    @Test
    fun `When email lookup call fails then useLink is false`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.failure(APIConnectionException()))

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.useLink).isEqualTo(false)

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(mock()))

            viewModel.emailController.onRawValueChange("valid2@email.com")

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.useLink).isEqualTo(true)
        }

    @Test
    fun `When entered existing account then it emits user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            val linkAccount = LinkAccount(
                mockConsumerSessionWithVerificationSession(
                    ConsumerSession.VerificationSession.SessionType.Sms,
                    ConsumerSession.VerificationSession.SessionState.Started
                )
            )
            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(linkAccount))

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isEqualTo(UserInput.SignIn(email))
        }

    @Test
    fun `When entered non-existing account then it collects phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isNull()
            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingPhoneOrName)
        }

    @Test
    fun `When user input becomes invalid then it emits null user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            assertThat(viewModel.viewState.value.userInput).isNull()

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isNull()

            val phone = "1234567890"
            viewModel.phoneController.onRawValueChange(phone)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(UserInput.SignUp(email, "+1$phone", "US", name = null))

            viewModel.phoneController.onRawValueChange("")

            assertThat(viewModel.viewState.value.userInput).isNull()
        }

    @Test
    fun `When user checks box then analytics event is sent`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()

            verify(linkEventsReporter).onInlineSignupCheckboxChecked()
        }

    @Test
    fun `When signup starts then analytics event is sent`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(Debouncer.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingPhoneOrName)
            verify(linkEventsReporter).onSignupStarted(true)
        }

    @Test
    fun `User input is valid without name for US users`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(countryCode = CountryCode.US)

        viewModel.toggleExpanded()
        viewModel.emailController.onRawValueChange("valid@email.com")
        viewModel.phoneController.onRawValueChange("1234567890")

        assertThat(viewModel.viewState.value.userInput).isEqualTo(
            UserInput.SignUp(
                email = "valid@email.com",
                phone = "+11234567890",
                country = CountryCode.US.value,
                name = null
            )
        )
    }

    @Test
    fun `User input is only valid with name for non-US users`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(countryCode = CountryCode.CA)

            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")
            viewModel.phoneController.selectCanadianPhoneNumber()
            viewModel.phoneController.onRawValueChange("1234567890")

            assertThat(viewModel.viewState.value.userInput).isNull()

            viewModel.nameController.onRawValueChange("Someone from Canada")

            assertThat(viewModel.viewState.value.userInput).isEqualTo(
                UserInput.SignUp(
                    email = "valid@email.com",
                    phone = "+11234567890",
                    country = CountryCode.CA.value,
                    name = "Someone from Canada"
                )
            )
        }

    @Test
    fun `Prefilled values are handled correctly`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            countryCode = CountryCode.GB,
            prefilledEmail = CUSTOMER_EMAIL,
            prefilledName = CUSTOMER_NAME,
            prefilledPhone = "+44$CUSTOMER_PHONE"
        )

        viewModel.toggleExpanded()

        val expectedInput = UserInput.SignUp(
            email = CUSTOMER_EMAIL,
            phone = "+44$CUSTOMER_PHONE",
            country = CountryCode.GB.value,
            name = CUSTOMER_NAME
        )

        assertThat(viewModel.viewState.value.userInput).isEqualTo(expectedInput)
    }

    @Test
    fun `E-mail is required for user input to be valid`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            countryCode = CountryCode.GB,
            prefilledName = CUSTOMER_NAME,
            prefilledPhone = "+44$CUSTOMER_PHONE"
        )

        viewModel.toggleExpanded()
        assertThat(viewModel.viewState.value.userInput).isNull()

        viewModel.emailController.onRawValueChange("finally_an_email_address@internet.cool")
        assertThat(viewModel.viewState.value.userInput).isNotNull()
    }

    private fun createViewModel(
        countryCode: CountryCode = CountryCode.US,
        prefilledEmail: String? = null,
        prefilledName: String? = null,
        prefilledPhone: String? = null
    ) = InlineSignupViewModel(
        config = LinkConfiguration(
            stripeIntent = mockStripeIntent(countryCode),
            merchantName = MERCHANT_NAME,
            merchantCountryCode = "US",
            customerInfo = LinkConfiguration.CustomerInfo(
                email = prefilledEmail,
                phone = prefilledPhone,
                name = prefilledName,
                billingCountryCode = null,
                shouldPrefill = true,
            ),
            shippingValues = null,
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            passthroughModeEnabled = false,
        ),
        linkAccountManager = linkAccountManager,
        linkEventsReporter = linkEventsReporter,
        logger = Logger.noop()
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

    private fun mockStripeIntent(
        countryCode: CountryCode = CountryCode.US
    ): PaymentIntent = mock {
        on { this.countryCode } doReturn countryCode.value
    }

    private fun PhoneNumberController.selectCanadianPhoneNumber() {
        val canada = countryDropdownController.displayItems.indexOfFirst {
            it.contains("Canada")
        }
        onSelectedCountryIndex(canada)
    }

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
        const val CUSTOMER_PHONE = "1234567890"
        const val CUSTOMER_NAME = "Customer"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "US"
    }
}
