package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAuth
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.android.utils.FakeLinkComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.Optional
import javax.inject.Provider
import kotlin.jvm.optionals.getOrNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkControllerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val logger = FakeLogger()
    private val linkConfigurationLoader = FakeLinkConfigurationLoader()
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val linkAccountManager = FakeLinkAccountManager(linkAccountHolder)
    private val linkAttestationCheck = FakeLinkAttestationCheck()
    private val linkAuth = FakeLinkAuth()
    private val linkComponent =
        FakeLinkComponent(
            linkAccountManager = linkAccountManager,
            linkAttestationCheck = linkAttestationCheck,
            linkAuth = linkAuth,
        )
    private val linkComponentBuilderProvider: Provider<LinkComponent.Builder> =
        Provider { FakeLinkComponent.Builder(linkComponent) }
    private val controllerComponentFactory: LinkControllerComponent.Factory = mock()

    @Test
    fun `Initial state is correct`() = runTest {
        val viewModel = createViewModel()

        viewModel.state(application).test {
            assertThat(awaitItem()).isEqualTo(
                LinkController.State()
            )
        }
    }

    @Test
    fun `state is updated when account changes`() = runTest {
        val viewModel = createViewModel()

        viewModel.state(application).test {
            awaitItem().run {
                assertThat(isConsumerVerified).isNull()
                assertThat(internalLinkAccount).isNull()
            }

            linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

            awaitItem().run {
                assertThat(isConsumerVerified).isTrue()
                assertThat(internalLinkAccount).isEqualTo(
                    LinkController.LinkAccount(
                        email = TestFactory.LINK_ACCOUNT.email,
                        redactedPhoneNumber = TestFactory.LINK_ACCOUNT.redactedPhoneNumber,
                        sessionState = LinkController.SessionState.LoggedIn,
                        consumerSessionClientSecret = TestFactory.LINK_ACCOUNT.clientSecret,
                    )
                )
            }

            val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
            )
            linkAccountHolder.set(LinkAccountUpdate.Value(LinkAccount(unverifiedSession)))

            awaitItem().run {
                assertThat(isConsumerVerified).isFalse()
                assertThat(internalLinkAccount?.sessionState).isEqualTo(LinkController.SessionState.NeedsVerification)
            }

            linkAccountHolder.set(LinkAccountUpdate.Value(null))

            awaitItem().run {
                assertThat(isConsumerVerified).isNull()
                assertThat(internalLinkAccount).isNull()
            }
        }
    }

    @Test
    fun `configure() sets new configuration and loads it`() = runTest {
        val viewModel = createViewModel()

        val loadedConfiguration = LinkTestUtils.createLinkConfiguration()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        assertThat(viewModel.configure(createControllerConfig())).isEqualTo(LinkController.ConfigureResult.Success)
        assertThat(linkComponent.configuration).isEqualTo(loadedConfiguration)
    }

    @Test
    fun `configure() fails when loader fails`() = runTest {
        val error = Exception("Failed to load")
        val viewModel = createViewModel()

        linkConfigurationLoader.linkConfigurationResult = Result.failure(error)

        assertThat(viewModel.configure(createControllerConfig()))
            .isEqualTo(LinkController.ConfigureResult.Failed(error))
    }

    @Test
    fun `configure() resets state`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateState {
            it.copy(createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

        val loadedConfiguration = LinkTestUtils.createLinkConfiguration()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        viewModel.state(application).test {
            assertThat(awaitItem()).isNotEqualTo(LinkController.State())
            assertThat(viewModel.configure(createControllerConfig())).isEqualTo(LinkController.ConfigureResult.Success)
            assertThat(awaitItem()).isEqualTo(LinkController.State())
        }
    }

    @Test
    fun `onPresentPaymentMethods() fails when configuration is not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onPresentPaymentMethods(mock(), "test@example.com")

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.PresentPaymentMethodsResult.Failed::class.java)
            val error = (result as LinkController.PresentPaymentMethodsResult.Failed).error
            assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when configuration is not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
            val error = (result as LinkController.CreatePaymentMethodResult.Failed).error
            assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when selectedPaymentMethod is not set`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
            val error = (result as LinkController.CreatePaymentMethodResult.Failed).error
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `onCreatePaymentMethod() succeeds when not in passthrough mode`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = "123",
                    billingPhone = null
                )
            )
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        linkAccountManager.createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            assertThat(awaitItem()).isEqualTo(LinkController.CreatePaymentMethodResult.Success)
        }

        viewModel.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when not in passthrough mode`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = "123",
                    billingPhone = null
                )
            )
        }

        val error = Exception("Error")
        linkAccountManager.createPaymentMethodResult = Result.failure(error)

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
            assertThat((result as LinkController.CreatePaymentMethodResult.Failed).error).isEqualTo(error)
        }

        viewModel.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onCreatePaymentMethod() in passthrough mode on success stores payment method`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel, passthroughModeEnabled = true)
        signIn()

        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = "123",
                    billingPhone = null
                )
            )
        }

        linkAccountManager.sharePaymentDetails = Result.success(TestFactory.LINK_SHARE_PAYMENT_DETAILS)
        val paymentMethod = PaymentMethodJsonParser().parse(
            JSONObject(TestFactory.LINK_SHARE_PAYMENT_DETAILS.encodedPaymentMethod)
        )

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            assertThat(awaitItem()).isEqualTo(LinkController.CreatePaymentMethodResult.Success)
        }

        viewModel.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when in passthrough mode`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel, passthroughModeEnabled = true)
        signIn()

        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = "123",
                    billingPhone = null
                )
            )
        }

        val error = Exception("Error")
        linkAccountManager.sharePaymentDetails = Result.failure(error)

        viewModel.createPaymentMethodResultFlow.test {
            viewModel.onCreatePaymentMethod()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
            assertThat((result as LinkController.CreatePaymentMethodResult.Failed).error).isEqualTo(error)
        }

        viewModel.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onLookupConsumer() on success emits success result`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        linkAccountManager.lookupConsumerResult = Result.success(TestFactory.LINK_ACCOUNT)

        viewModel.lookupConsumerResultFlow.test {
            viewModel.onLookupConsumer("test@example.com")
            assertThat(awaitItem())
                .isEqualTo(LinkController.LookupConsumerResult.Success("test@example.com", true))
        }
    }

    @Test
    fun `onLookupConsumer() on failure emits failure result`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val error = Exception("Error")
        linkAuth.lookupResult = LinkAuthResult.Error(error)

        viewModel.lookupConsumerResultFlow.test {
            viewModel.onLookupConsumer("test@example.com")
            val result = awaitItem()
            assertThat(result).isEqualTo(
                LinkController.LookupConsumerResult.Failed("test@example.com", error)
            )
        }
    }

    @Test
    fun `onPresentPaymentMethods() launches Link with correct arguments`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher, "test@example.com")

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.startWithVerificationDialog).isTrue()
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode)
            .isEqualTo(
                LinkLaunchMode.PaymentMethodSelection(
                    selectedPayment = null,
                    sharePaymentDetailsImmediatelyAfterCreation = false
                )
            )

        val state = viewModel.state(application).first()
        assertThat(state.isConsumerVerified).isNull()
    }

    @Test
    fun `onPresentPaymentMethods() on matching email passes existing account`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher, TestFactory.LINK_ACCOUNT.email)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isEqualTo(TestFactory.LINK_ACCOUNT)
    }

    @Test
    fun `onPresentPaymentMethods() on non-matching email clears account state`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = "123",
                    billingPhone = null
                ),
                createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        }

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher, "another@email.com")

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isNull()

        viewModel.state(application).test {
            val state = awaitItem()
            assertThat(state.selectedPaymentMethodPreview).isNull()
            assertThat(state.createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onPresentPaymentMethods() with email includes the email in the launch config`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val newEmail = "new@email.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher = launcher, email = newEmail)

        val customerInfo = launcher.calls.awaitItem().input.configuration.customerInfo
        assertThat(customerInfo.email).isEqualTo(newEmail)
    }

    @Test
    fun `onPresentPaymentMethods() with null email uses existing customer info`() = runTest {
        val viewModel = createViewModel()
        val billingDetails = PaymentSheet.BillingDetails(
            address = null,
            email = TestFactory.CUSTOMER_EMAIL,
            name = TestFactory.CUSTOMER_NAME,
            phone = TestFactory.CUSTOMER_PHONE,
        )
        configure(viewModel, defaultBillingDetails = Optional.of(billingDetails))

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher = launcher, email = null)

        val customerInfo = launcher.calls.awaitItem().input.configuration.customerInfo
        assertThat(customerInfo.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(customerInfo.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(customerInfo.phone).isEqualTo(TestFactory.CUSTOMER_PHONE)
    }

    @Test
    fun `onLinkActivityResult() with PaymentMethodObtained result does nothing`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val initialState = viewModel.state(application).first()
        val initialAccount = linkAccountHolder.linkAccountInfo.first()

        // First call onPresentPaymentMethods to set up the launch mode
        viewModel.onPresentPaymentMethods(FakeActivityResultLauncher(), "test@example.com")

        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onLinkActivityResult(
                LinkActivityResult.PaymentMethodObtained(
                    paymentMethod = mock()
                )
            )
            expectNoEvents()
        }

        assertThat(viewModel.state(application).first()).isEqualTo(initialState)
        assertThat(linkAccountHolder.linkAccountInfo.first()).isEqualTo(initialAccount)
    }

    @Test
    fun `onLinkActivityResult() with Canceled result`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // First call onPresentPaymentMethods to set up the launch mode
        viewModel.onPresentPaymentMethods(FakeActivityResultLauncher(), "test@example.com")

        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onLinkActivityResult(
                LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                )
            )
            assertThat(awaitItem()).isEqualTo(LinkController.PresentPaymentMethodsResult.Canceled)
        }
    }

    @Test
    fun `onLinkActivityResult() on Completed result emits Success and updates preview`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // First call onPresentPaymentMethods to set up the launch mode
        viewModel.onPresentPaymentMethods(FakeActivityResultLauncher(), "test@example.com")

        val linkPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = "123",
            billingPhone = null
        )
        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onLinkActivityResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                    selectedPayment = linkPaymentMethod,
                    shippingAddress = null,
                )
            )
            assertThat(awaitItem()).isEqualTo(LinkController.PresentPaymentMethodsResult.Success)
        }

        viewModel.state(application).test {
            assertThat(awaitItem().selectedPaymentMethodPreview?.sublabel)
                .isEqualTo("Visa Credit •••• 4242")
        }
    }

    @Test
    fun `onLinkActivityResult() with Failed result`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // First call onPresentPaymentMethods to set up the launch mode
        viewModel.onPresentPaymentMethods(FakeActivityResultLauncher(), "test@example.com")

        val error = Exception("Error")
        viewModel.presentPaymentMethodsResultFlow.test {
            viewModel.onLinkActivityResult(
                LinkActivityResult.Failed(
                    error = error,
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                )
            )
            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.PresentPaymentMethodsResult.Failed::class.java)
            assertThat((result as LinkController.PresentPaymentMethodsResult.Failed).error).isEqualTo(error)
        }
    }

    @Test
    fun `onLinkActivityResult() on account cleared clears verification state`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        viewModel.onLinkActivityResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )

        viewModel.state(application).test {
            assertThat(awaitItem().isConsumerVerified).isNull()
        }
    }

    @Test
    fun `onAuthenticate() fails when configuration is not set`() = runTest {
        val viewModel = createViewModel()

        viewModel.authenticationResultFlow.test {
            viewModel.onAuthenticate(mock(), "test@example.com")
            val result = awaitItem() as LinkController.AuthenticationResult.Failed
            assertThat(result.error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onAuthenticate() launches Link with correct arguments`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, "test@example.com")

        val args = launcher.calls.awaitItem().input
        assertThat(args.startWithVerificationDialog).isTrue()
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())

        val state = viewModel.state(application).first()
        assertThat(state.isConsumerVerified).isNull()
    }

    @Test
    fun `onAuthenticate() on matching email passes existing account`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        configure(viewModel)

        // Create an unverified account (one that needs verification)
        val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
        )
        val unverifiedAccount = LinkAccount(unverifiedSession)
        linkAccountHolder.set(LinkAccountUpdate.Value(unverifiedAccount))

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, unverifiedAccount.email)

        val args = launcher.calls.awaitItem().input
        assertThat(args.linkAccountInfo.account).isEqualTo(unverifiedAccount)
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() on non-matching email clears account state`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, "another@email.com")

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() updates state with presentedForEmail and currentLaunchMode`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val email = "test@example.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, email)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.configuration.customerInfo.email).isEqualTo(email)
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() handles configuration loading failure`() = runTest {
        val viewModel = createViewModel()
        // Don't configure the viewModel, so it will fail with MissingConfigurationException

        viewModel.authenticationResultFlow.test {
            viewModel.onAuthenticate(mock(), "test@example.com")

            val result = awaitItem() as LinkController.AuthenticationResult.Failed
            assertThat(result.error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onAuthenticate() with null email passes null to configuration`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, null)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.configuration.customerInfo.email).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() sets correct customer info in configuration`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val email = "test@example.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, email)

        val call = launcher.calls.awaitItem()
        val args = call.input
        val customerInfo = args.configuration.customerInfo
        assertThat(customerInfo.email).isEqualTo(email)
        assertThat(customerInfo.name).isNull()
        assertThat(customerInfo.phone).isNull()
        assertThat(customerInfo.billingCountryCode).isNull()
    }

    @Test
    fun `onAuthenticateExistingConsumer() launches Link with existingOnly flag set to true`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticateExistingConsumer(launcher, "test@example.com")

        val args = launcher.calls.awaitItem().input
        assertThat(args.startWithVerificationDialog).isTrue()
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication(existingOnly = true))
    }

    @Test
    fun `onAuthenticateExistingConsumer() on succeeds immediately with matching email`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        viewModel.authenticationResultFlow.test {
            val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
            viewModel.onAuthenticateExistingConsumer(launcher, TestFactory.LINK_ACCOUNT.email)

            assertThat(awaitItem()).isEqualTo(LinkController.AuthenticationResult.Success)
        }
    }

    @Test
    fun `getLinkAccountInfo() clears account holder when account is null`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // Set up an existing account
        signIn()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(TestFactory.LINK_ACCOUNT)

        // Call onAuthenticate with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, "different@email.com")

        // Verify that the account holder was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()

        // Verify that the launcher was called with null account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isNull()
    }

    @Test
    fun `getLinkAccountInfo() does not clear account holder when account is not null`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // Create an unverified account (one that needs verification)
        val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
        )
        val unverifiedAccount = LinkAccount(unverifiedSession)
        linkAccountHolder.set(LinkAccountUpdate.Value(unverifiedAccount))
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(unverifiedAccount)

        // Call onAuthenticate with a matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onAuthenticate(launcher, unverifiedAccount.email)

        // Verify that the account holder was NOT cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(unverifiedAccount)

        // Verify that the launcher was called with the existing account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isEqualTo(unverifiedAccount)
    }

    @Test
    fun `onAuthenticate() on non-matching email clears saved payment data`() = runTest {
        testAuthenticationClearsSavedPaymentData { viewModel, launcher, email ->
            viewModel.onAuthenticate(launcher, email)
        }
    }

    @Test
    fun `onAuthenticateExistingConsumer() on non-matching email clears saved payment data`() = runTest {
        testAuthenticationClearsSavedPaymentData { viewModel, launcher, email ->
            viewModel.onAuthenticateExistingConsumer(launcher, email)
        }
    }

    @Test
    fun `onPresentPaymentMethods() clears account holder when email doesn't match`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        // Set up an existing account
        signIn()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(TestFactory.LINK_ACCOUNT)

        // Call onPresentPaymentMethods with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        viewModel.onPresentPaymentMethods(launcher, "different@email.com")

        // Verify that the account holder was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()

        // Verify that the launcher was called with null account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isNull()
    }

    @Test
    fun `onRegisterConsumer() on success emits success result and updates account`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)

        val expectedAccount = TestFactory.LINK_ACCOUNT
        linkAuth.signupResult = LinkAuthResult.Success(expectedAccount)

        val email = "test@example.com"
        val phone = "1234567890"
        val country = "US"
        val name = "Test User"
        viewModel.registerConsumerResultFlow.test {
            viewModel.onRegisterConsumer(
                email = email,
                phone = phone,
                country = country,
                name = name
            )

            val result = awaitItem()
            assertThat(result).isEqualTo(LinkController.RegisterConsumerResult.Success)
        }

        // Verify that the account was updated
        val account = linkAccountHolder.linkAccountInfo.value.account
        assertThat(account).isEqualTo(expectedAccount)
    }

    @Test
    fun `onRegisterConsumer() on failure emits failure result and clears account`() = runTest {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        val email = "test@example.com"
        val phone = "1234567890"
        val country = "US"
        val name = "Test User"
        val error = Exception("Registration failed")

        linkAuth.signupResult = LinkAuthResult.Error(error)

        viewModel.registerConsumerResultFlow.test {
            viewModel.onRegisterConsumer(
                email = email,
                phone = phone,
                country = country,
                name = name
            )

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.RegisterConsumerResult.Failed::class.java)
            assertThat((result as LinkController.RegisterConsumerResult.Failed).error).isEqualTo(error)
        }

        // Verify that the account was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    private suspend fun testAuthenticationClearsSavedPaymentData(
        authenticateCall: (
            viewModel: LinkControllerViewModel,
            launcher: FakeActivityResultLauncher<LinkActivityContract.Args>,
            email: String
        ) -> Unit
    ) {
        val viewModel = createViewModel()
        configure(viewModel)
        signIn()

        // Set up some saved payment data
        val selectedPaymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = "123",
            billingPhone = null
        )
        val createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        viewModel.updateState {
            it.copy(
                selectedPaymentMethod = selectedPaymentMethod,
                createdPaymentMethod = createdPaymentMethod
            )
        }

        // Verify initial state has the saved data
        viewModel.state(application).test {
            val initialState = awaitItem()
            assertThat(initialState.selectedPaymentMethodPreview).isNotNull()
            assertThat(initialState.createdPaymentMethod).isEqualTo(createdPaymentMethod)
        }

        // Call the authentication method with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        authenticateCall(viewModel, launcher, "another@email.com")

        // Verify that saved payment data is cleared
        viewModel.state(application).test {
            val finalState = awaitItem()
            assertThat(finalState.selectedPaymentMethodPreview).isNull()
            assertThat(finalState.createdPaymentMethod).isNull()
        }
    }

    private fun createViewModel(): LinkControllerViewModel {
        return LinkControllerViewModel(
            application = application,
            logger = logger,
            linkConfigurationLoader = linkConfigurationLoader,
            linkAccountHolder = linkAccountHolder,
            linkComponentBuilderProvider = linkComponentBuilderProvider,
            controllerComponentFactory = controllerComponentFactory
        )
    }

    private suspend fun configure(
        viewModel: LinkControllerViewModel,
        passthroughModeEnabled: Boolean = false,
        defaultBillingDetails: Optional<PaymentSheet.BillingDetails>? = null,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration? = null,
    ) {
        linkConfigurationLoader.shouldUpdateResult = true
        val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
            merchantName = "Test",
            passthroughModeEnabled = passthroughModeEnabled
        )
        linkConfigurationLoader.linkConfigurationResult = Result.success(linkConfiguration)
        viewModel.configure(
            LinkController.Configuration.Builder("Test")
                .apply { defaultBillingDetails?.let { defaultBillingDetails(it.getOrNull()) } }
                .apply { billingDetailsCollectionConfiguration?.let { billingDetailsCollectionConfiguration(it) } }
                .build()
        )
    }

    private fun createControllerConfig() = LinkController.Configuration.default(application)

    private fun signIn() {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
    }
}
