package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InlineSignupViewModelTest {
    private val linkAccountManager = FakeLinkAccountManager()
    private val linkEventsReporter = mock<LinkEventsReporter>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `When email and phone are provided it should prefill all values`() =
        runTest(UnconfinedTestDispatcher()) {
            val linkAccountManager = object : FakeLinkAccountManager() {
                var counter = 0

                override suspend fun lookupConsumer(email: String, startSession: Boolean): Result<LinkAccount?> {
                    counter += 1
                    return super.lookupConsumer(email, startSession)
                }
            }
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
                    ),
                    shippingDetails = null,
                    passthroughModeEnabled = false,
                    flags = emptyMap(),
                    cardBrandChoice = null,
                ),
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                linkAccountManager = linkAccountManager,
                linkEventsReporter = linkEventsReporter,
                logger = Logger.noop(),
            )

            linkAccountManager.lookupConsumerResult = Result.success(null)

            viewModel.toggleExpanded()
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 1) // Trigger lookup by waiting for delay.
            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
            assertThat(viewModel.emailController.fieldValue.first()).isEqualTo(CUSTOMER_EMAIL)
            assertThat(viewModel.phoneController.fieldValue.first()).isEqualTo(CUSTOMER_PHONE)

            assertThat(linkAccountManager.counter).isEqualTo(1)
        }

    @Test
    fun `When email lookup call fails then useLink is false`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            linkAccountManager.lookupConsumerResult = Result.failure(APIConnectionException())

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.useLink).isEqualTo(false)

            linkAccountManager.lookupConsumerResult = Result.success(mock())

            viewModel.emailController.onRawValueChange("valid2@email.com")

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

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
            linkAccountManager.lookupConsumerResult = Result.success(linkAccount)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isEqualTo(UserInput.SignIn(email))
        }

    @Test
    fun `When entered non-existing account then it collects phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            linkAccountManager.lookupConsumerResult = Result.success(null)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isNull()
            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
        }

    @Test
    fun `When user input becomes invalid then it emits null user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            assertThat(viewModel.viewState.value.userInput).isNull()

            linkAccountManager.lookupConsumerResult = Result.success(null)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.userInput).isNull()

            val phone = "1234567890"
            viewModel.phoneController.onRawValueChange(phone)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        email = email,
                        phone = "+1$phone",
                        country = "US",
                        name = null,
                        consentAction = SignUpConsentAction.Checkbox
                    )
                )

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

            linkAccountManager.lookupConsumerResult = Result.success(null)

            // Advance past lookup debounce delay
            advanceTimeBy(LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.viewState.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
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
                name = null,
                consentAction = SignUpConsentAction.Checkbox
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
                    name = "Someone from Canada",
                    consentAction = SignUpConsentAction.Checkbox
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
            name = CUSTOMER_NAME,
            consentAction = SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
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

    @Test
    fun `Action is 'Checkbox' when 'InsteadOfSaveForFutureUse' and no prefilled input`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                countryCode = CountryCode.US,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            )

            viewModel.nameController.onValueChange(CUSTOMER_NAME)
            viewModel.emailController.onValueChange(CUSTOMER_EMAIL)
            viewModel.phoneController.onValueChange(CUSTOMER_PHONE)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = "+1$CUSTOMER_PHONE",
                        country = CountryCode.US.value,
                        consentAction = SignUpConsentAction.Checkbox
                    )
                )
        }

    @Test
    fun `Action is 'CheckboxWithPrefilledEmail' when 'InsteadOfSaveForFutureUse' and prefilled email`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                prefilledEmail = CUSTOMER_EMAIL,
                countryCode = CountryCode.US,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            )

            viewModel.nameController.onValueChange(CUSTOMER_NAME)
            viewModel.phoneController.onValueChange(CUSTOMER_PHONE)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = "+1$CUSTOMER_PHONE",
                        country = CountryCode.US.value,
                        consentAction = SignUpConsentAction.CheckboxWithPrefilledEmail
                    )
                )
        }

    @Test
    fun `Action is 'CheckboxWithPrefilledEmailAndPhone' when 'InsteadOfSaveForFutureUse' and filled email & phone`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                prefilledEmail = CUSTOMER_EMAIL,
                prefilledPhone = "+1$CUSTOMER_PHONE",
                countryCode = CountryCode.US,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            )

            viewModel.nameController.onValueChange(CUSTOMER_NAME)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = "+1$CUSTOMER_PHONE",
                        country = CountryCode.US.value,
                        consentAction = SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
                    )
                )
        }

    @Test
    fun `Action is 'Implied' when 'InsteadOfSaveForFutureUse'`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                countryCode = CountryCode.US,
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            )

            viewModel.nameController.onValueChange(CUSTOMER_NAME)
            viewModel.emailController.onValueChange(CUSTOMER_EMAIL)
            viewModel.phoneController.onValueChange(CUSTOMER_PHONE)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = "+1$CUSTOMER_PHONE",
                        country = CountryCode.US.value,
                        consentAction = SignUpConsentAction.Implied
                    )
                )
        }

    @Test
    fun `Action is 'ImpliedWithPrefilledEmail' when 'InsteadOfSaveForFutureUse' & prefilled email`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                countryCode = CountryCode.US,
                prefilledEmail = CUSTOMER_EMAIL,
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            )

            viewModel.nameController.onValueChange(CUSTOMER_NAME)
            viewModel.phoneController.onValueChange(CUSTOMER_PHONE)

            assertThat(viewModel.viewState.value.userInput)
                .isEqualTo(
                    UserInput.SignUp(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = "+1$CUSTOMER_PHONE",
                        country = CountryCode.US.value,
                        consentAction = SignUpConsentAction.ImpliedWithPrefilledEmail
                    )
                )
        }

    private fun createViewModel(
        countryCode: CountryCode = CountryCode.US,
        prefilledEmail: String? = null,
        prefilledName: String? = null,
        prefilledPhone: String? = null,
        signupMode: LinkSignupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
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
            ),
            shippingDetails = null,
            passthroughModeEnabled = false,
            flags = emptyMap(),
            cardBrandChoice = null,
        ),
        signupMode = signupMode,
        linkAccountManager = linkAccountManager,
        linkEventsReporter = linkEventsReporter,
        logger = Logger.noop(),
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
        countryDropdownController.onValueChange(canada)
    }

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
        const val CUSTOMER_PHONE = "1234567890"
        const val CUSTOMER_NAME = "Customer"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "US"
    }
}
