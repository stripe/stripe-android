package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.FakeSignupToLinkToggleInteractor
import com.stripe.android.utils.FakeLinkComponent
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class SignupForLinkTest {

    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val signupToLinkToggleInteractor = FakeSignupToLinkToggleInteractor()
    private val linkAccountManager = FakeLinkAccountManager()
    private val linkComponent = FakeLinkComponent(linkAccountManager = linkAccountManager)
    private val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(component = linkComponent)
    private val linkHandler = LinkHandler(linkConfigurationCoordinator)

    private val eventReporter = FakeEventReporter()

    private val signupForLink = SignupForLink(
        linkAccountHolder = linkAccountHolder,
        linkHandler = linkHandler,
        signupToLinkToggleInteractor = signupToLinkToggleInteractor,
        eventReporter = eventReporter
    )

    private val testEmail = "test@example.com"
    private val testPhone = "+1234567890"
    private val testName = "John Doe"
    private val testCountry = "US"

    private val testAddress = Address(
        line1 = "123 Main St",
        line2 = null,
        city = "San Francisco",
        state = "CA",
        postalCode = "94105",
        country = testCountry
    )

    private val testBillingDetails = PaymentMethod.BillingDetails(
        address = testAddress,
        email = testEmail,
        name = testName,
        phone = testPhone
    )

    private val testPaymentSelection = PaymentSelection.New.Card(
        paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
            billingDetails = testBillingDetails
        ),
        brand = CardBrand.Visa,
        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
    )

    private val testLinkConfiguration = mock<LinkConfiguration>()

    @Test
    fun `invoke should return early when signup toggle is disabled`() = runTest {
        // Given
        signupToLinkToggleInteractor.setSignupToLinkValue(false)

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        // Verify that signup was not attempted
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `invoke should return early when link account already exists`() = runTest {
        // Given
        signupToLinkToggleInteractor.setSignupToLinkValue(true)
        val existingAccount = mock<LinkAccount>()
        val accountInfo = LinkAccountUpdate.Value(existingAccount)
        linkAccountHolder.set(accountInfo)

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        // Verify that signup was not attempted
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `invoke should return early when linkConfiguration is null`() = runTest {
        // Given
        signupToLinkToggleInteractor.setSignupToLinkValue(true)
        linkAccountHolder.set(LinkAccountUpdate.Value(null))

        // When
        signupForLink(null, testPaymentSelection)

        // Then
        // Verify that signup was not attempted
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `invoke should return early when email is null`() = runTest {
        // Given
        signupToLinkToggleInteractor.setSignupToLinkValue(true)
        linkAccountHolder.set(LinkAccountUpdate.Value(null))

        val paymentSelectionWithoutEmail = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                billingDetails = testBillingDetails.copy(email = null)
            ),
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // When
        signupForLink(testLinkConfiguration, paymentSelectionWithoutEmail)

        // Then
        // Verify that signup was not attempted
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `invoke should return early when paymentSelection is null`() = runTest {
        // Given
        signupToLinkToggleInteractor.setSignupToLinkValue(true)
        linkAccountHolder.set(LinkAccountUpdate.Value(null))

        // When
        signupForLink(testLinkConfiguration, null)

        // Then
        // Verify that signup was not attempted
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `invoke should successfully sign up to Link and create card payment details`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        val expectedUserInput = UserInput.SignUpOptionalPhone(
            email = testEmail,
            country = testCountry,
            phone = testPhone,
            name = testName,
            consentAction = SignUpConsentAction.Implied
        )

        linkAccountManager.signInWithUserInputResult = Result.success(TestFactory.LINK_ACCOUNT)
        linkAccountManager.createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        val signInCall = linkAccountManager.awaitSignInWithUserInputCall()
        assertThat(signInCall).isEqualTo(expectedUserInput)

        val createCardCall = linkAccountManager.awaitCreateCardPaymentDetailsCall()
        assertThat(createCardCall).isEqualTo(testPaymentSelection.paymentMethodCreateParams)
    }

    @Test
    fun `invoke should use default country US when billing address country is null`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        val paymentSelectionWithoutCountry = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                billingDetails = testBillingDetails.copy(
                    address = testAddress.copy(country = null)
                )
            ),
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val expectedUserInput = UserInput.SignUpOptionalPhone(
            email = testEmail,
            country = "US", // Default country
            phone = testPhone,
            name = testName,
            consentAction = SignUpConsentAction.Implied
        )

        linkAccountManager.signInWithUserInputResult = Result.success(TestFactory.LINK_ACCOUNT)

        // When
        signupForLink(testLinkConfiguration, paymentSelectionWithoutCountry)

        // Then
        val signInCall = linkAccountManager.awaitSignInWithUserInputCall()
        assertThat(signInCall).isEqualTo(expectedUserInput)
    }

    @Test
    fun `invoke should handle signup failure gracefully`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        val error = com.stripe.android.core.exception.APIException(message = "Signup failed")
        linkAccountManager.signInWithUserInputResult = Result.failure(error)

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        linkAccountManager.awaitSignInWithUserInputCall()
    }

    @Test
    fun `invoke should handle card payment details creation failure gracefully`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        linkAccountManager.signInWithUserInputResult = Result.success(TestFactory.LINK_ACCOUNT)
        val error = com.stripe.android.core.exception.APIException(message = "Card creation failed")
        linkAccountManager.createCardPaymentDetailsResult = Result.failure(error)

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        linkAccountManager.awaitSignInWithUserInputCall()
        linkAccountManager.awaitCreateCardPaymentDetailsCall()
    }

    @Test
    fun `invoke should not create card payment details for non-New payment selections`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        linkAccountManager.signInWithUserInputResult = Result.success(TestFactory.LINK_ACCOUNT)

        // Use TestFactory to create a saved payment method with proper billing details
        val paymentMethodWithBillingDetails = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            billingDetails = testBillingDetails
        )
        val savedPaymentSelection = PaymentSelection.Saved(paymentMethodWithBillingDetails)

        val expectedUserInput = UserInput.SignUpOptionalPhone(
            email = testEmail,
            country = testCountry,
            phone = testPhone,
            name = testName,
            consentAction = SignUpConsentAction.Implied
        )

        // When
        signupForLink(testLinkConfiguration, savedPaymentSelection)

        // Then
        // Should still sign up to Link with user input (since we have email)
        val signInCall = linkAccountManager.awaitSignInWithUserInputCall()
        assertThat(signInCall).isEqualTo(expectedUserInput)

        // But should NOT create card payment details since it's not a New payment selection
        // No more invocations should be made to createCardPaymentDetails
    }

    @Test
    fun `invoke should handle exceptions during linkAccountManager operations`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        // Make the linkAccountManager throw an exception during signInWithUserInput
        linkAccountManager.signInWithUserInputResult = Result.failure(
            com.stripe.android.core.exception.APIException(message = "Network error")
        )

        // When
        signupForLink(testLinkConfiguration, testPaymentSelection)

        // Then
        linkAccountManager.awaitSignInWithUserInputCall()
    }

    @Test
    fun `invoke should work with minimal billing details`() = runTest {
        // Given
        setupSuccessfulSignupMocks()

        val minimalBillingDetails = PaymentMethod.BillingDetails(
            email = testEmail,
            address = null,
            name = null,
            phone = null
        )

        val paymentSelectionMinimal = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                billingDetails = minimalBillingDetails
            ),
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val expectedUserInput = UserInput.SignUpOptionalPhone(
            email = testEmail,
            country = "US", // Default
            phone = null,
            name = null,
            consentAction = SignUpConsentAction.Implied
        )

        linkAccountManager.signInWithUserInputResult = Result.success(TestFactory.LINK_ACCOUNT)

        // When
        signupForLink(testLinkConfiguration, paymentSelectionMinimal)

        // Then
        val signInCall = linkAccountManager.awaitSignInWithUserInputCall()
        assertThat(signInCall).isEqualTo(expectedUserInput)
    }

    private fun setupSuccessfulSignupMocks() {
        signupToLinkToggleInteractor.setSignupToLinkValue(true)
        linkAccountHolder.set(LinkAccountUpdate.Value(null))
    }
}
