package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.exceptions.AppAttestationException
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.LinkComponent
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

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkControllerInteractorTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val logger = FakeLogger()
    private val linkConfigurationLoader = FakeLinkConfigurationLoader()
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val linkAccountManager = FakeLinkAccountManager(linkAccountHolder)
    private val linkAttestationCheck = FakeLinkAttestationCheck()
    private val linkComponent =
        FakeLinkComponent(
            linkAccountManager = linkAccountManager,
            linkAttestationCheck = linkAttestationCheck,
        )
    private val linkComponentBuilderProvider: Provider<LinkComponent.Builder> =
        Provider { FakeLinkComponent.Builder(linkComponent) }

    @Test
    fun `Initial state is correct`() = runTest {
        val interactor = createInteractor()

        interactor.state(application).test {
            assertThat(awaitItem()).isEqualTo(
                LinkController.State()
            )
        }
    }

    @Test
    fun `state is updated when account changes`() = runTest {
        val interactor = createInteractor()

        interactor.state(application).test {
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
        val interactor = createInteractor()

        val loadedConfiguration = LinkTestUtils.createLinkConfiguration()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        val controllerConfig =
            LinkController.Configuration.Builder(
                merchantDisplayName = "Example",
                publishableKey = "pk_123",
                stripeAccountId = "acct_123"
            ).build()
        assertThat(interactor.configure(controllerConfig)).isEqualTo(LinkController.ConfigureResult.Success)
        assertThat(linkComponent.configuration).isEqualTo(loadedConfiguration)
        val paymentConfiguration = PaymentConfiguration.getInstance(application)
        assertThat(paymentConfiguration.publishableKey).isEqualTo(controllerConfig.publishableKey)
        assertThat(paymentConfiguration.stripeAccountId).isEqualTo(controllerConfig.stripeAccountId)
    }

    @Test
    fun `configure() fails when loader fails`() = runTest {
        val error = Exception("Failed to load")
        val interactor = createInteractor()

        linkConfigurationLoader.linkConfigurationResult = Result.failure(error)

        assertThat(interactor.configure(createControllerConfig()))
            .isEqualTo(LinkController.ConfigureResult.Failed(error))
    }

    @Test
    fun `configure() resets state`() = runTest {
        val interactor = createInteractor()

        interactor.updateState {
            it.copy(createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

        val loadedConfiguration = LinkTestUtils.createLinkConfiguration()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)

        interactor.state(application).test {
            assertThat(awaitItem()).isNotEqualTo(LinkController.State())
            assertThat(interactor.configure(createControllerConfig())).isEqualTo(LinkController.ConfigureResult.Success)
            assertThat(awaitItem()).isEqualTo(LinkController.State())
        }
    }

    @Test
    fun `configure() fails when attestation check fails with AttestationFailed`() = runTest {
        val interactor = createInteractor()
        val attestationError = Exception("Attestation failed")

        configureWithAttestation(interactor, LinkAttestationCheck.Result.AttestationFailed(attestationError))

        val result = interactor.configure(createControllerConfig())

        assertThat(result).isInstanceOf(LinkController.ConfigureResult.Failed::class.java)
        val failedResult = result as LinkController.ConfigureResult.Failed
        assertThat(failedResult.error).isInstanceOf(AppAttestationException::class.java)
        assertThat(failedResult.error.cause).isEqualTo(attestationError)
    }

    @Test
    fun `configure() fails when attestation check fails with AccountError`() = runTest {
        val interactor = createInteractor()
        val accountError = Exception("Account error")

        configureWithAttestation(interactor, LinkAttestationCheck.Result.AccountError(accountError))

        val result = interactor.configure(createControllerConfig())

        assertThat(result).isInstanceOf(LinkController.ConfigureResult.Failed::class.java)
        val failedResult = result as LinkController.ConfigureResult.Failed
        assertThat(failedResult.error).isEqualTo(accountError)
    }

    @Test
    fun `configure() fails when attestation check fails with Error`() = runTest {
        val interactor = createInteractor()
        val genericError = Exception("Generic error")

        configureWithAttestation(interactor, LinkAttestationCheck.Result.Error(genericError))

        val result = interactor.configure(createControllerConfig())

        assertThat(result).isInstanceOf(LinkController.ConfigureResult.Failed::class.java)
        val failedResult = result as LinkController.ConfigureResult.Failed
        assertThat(failedResult.error).isEqualTo(genericError)
    }

    @Test
    fun `onPresentPaymentMethods() fails when configuration is not set`() = runTest {
        val interactor = createInteractor()

        interactor.presentPaymentMethodsResultFlow.test {
            interactor.presentPaymentMethods(mock(), "test@example.com", null)

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkController.PresentPaymentMethodsResult.Failed::class.java)
            val error = (result as LinkController.PresentPaymentMethodsResult.Failed).error
            assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when configuration is not set`() = runTest {
        val interactor = createInteractor()

        val result = interactor.createPaymentMethod()

        assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
        val error = (result as LinkController.CreatePaymentMethodResult.Failed).error
        assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
    }

    @Test
    fun `onCreatePaymentMethod() fails when selectedPaymentMethod is not set`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val result = interactor.createPaymentMethod()

        assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
        val error = (result as LinkController.CreatePaymentMethodResult.Failed).error
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `onCreatePaymentMethod() succeeds when not in passthrough mode`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        interactor.updateState {
            it.copy(selectedPaymentMethod = createTestPaymentMethod())
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        linkAccountManager.createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)

        assertThat(interactor.createPaymentMethod())
            .isEqualTo(LinkController.CreatePaymentMethodResult.Success(paymentMethod))

        interactor.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when not in passthrough mode`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        interactor.updateState {
            it.copy(selectedPaymentMethod = createTestPaymentMethod())
        }

        val error = Exception("Error")
        linkAccountManager.createPaymentMethodResult = Result.failure(error)

        val result = interactor.createPaymentMethod()

        assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
        assertThat((result as LinkController.CreatePaymentMethodResult.Failed).error).isEqualTo(error)

        interactor.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onCreatePaymentMethod() in passthrough mode on success stores payment method`() = runTest {
        val interactor = createInteractor()
        configure(interactor, passthroughModeEnabled = true)
        signIn()

        interactor.updateState {
            it.copy(selectedPaymentMethod = createTestPaymentMethod())
        }

        linkAccountManager.sharePaymentDetails = Result.success(TestFactory.LINK_SHARE_PAYMENT_DETAILS)
        val paymentMethod = PaymentMethodJsonParser().parse(
            JSONObject(TestFactory.LINK_SHARE_PAYMENT_DETAILS.encodedPaymentMethod)
        )

        assertThat(interactor.createPaymentMethod())
            .isEqualTo(LinkController.CreatePaymentMethodResult.Success(paymentMethod))

        interactor.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onCreatePaymentMethod() fails when in passthrough mode`() = runTest {
        val interactor = createInteractor()
        configure(interactor, passthroughModeEnabled = true)
        signIn()

        interactor.updateState {
            it.copy(selectedPaymentMethod = createTestPaymentMethod())
        }

        val error = Exception("Error")
        linkAccountManager.sharePaymentDetails = Result.failure(error)

        val result = interactor.createPaymentMethod()
        assertThat(result).isInstanceOf(LinkController.CreatePaymentMethodResult.Failed::class.java)
        assertThat((result as LinkController.CreatePaymentMethodResult.Failed).error).isEqualTo(error)

        interactor.state(application).test {
            assertThat(awaitItem().createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onLookupConsumer() on success emits success result`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        linkAccountManager.lookupResult = Result.success(TestFactory.LINK_ACCOUNT)

        assertThat(interactor.lookupConsumer("test@example.com"))
            .isEqualTo(LinkController.LookupConsumerResult.Success("test@example.com", true))
    }

    @Test
    fun `onLookupConsumer() on failure emits failure result`() = runTest(dispatcher) {
        val interactor = createInteractor()
        configure(interactor)

        val error = Exception("Error")
        linkAccountManager.lookupResult = Result.failure(error)

        assertThat(interactor.lookupConsumer("test@example.com"))
            .isEqualTo(LinkController.LookupConsumerResult.Failed("test@example.com", error))
    }

    @Test
    fun `onLookupConsumer() on attestation failure emits failure result with AppAttestationException`() =
        runTest(dispatcher) {
            val interactor = createInteractor()
            configure(interactor)

            val attestationError = Exception("Attestation failed")
            linkAccountManager.lookupResult = Result.failure(AppAttestationException(attestationError))

            val result = interactor.lookupConsumer("test@example.com")
            assertThat(result).isInstanceOf(LinkController.LookupConsumerResult.Failed::class.java)
            val failedResult = result as LinkController.LookupConsumerResult.Failed
            assertThat(failedResult.email).isEqualTo("test@example.com")
            assertThat(failedResult.error).isInstanceOf(AppAttestationException::class.java)
            assertThat(failedResult.error.cause).isEqualTo(attestationError)
        }

    @Test
    fun `updatePhoneNumber() on success emits success result`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        linkAccountManager.updatePhoneNumberResult = Result.success(TestFactory.LINK_ACCOUNT)

        assertThat(interactor.updatePhoneNumber("+1234567890"))
            .isEqualTo(LinkController.UpdatePhoneNumberResult.Success)
    }

    @Test
    fun `updatePhoneNumber() on failure emits failure result`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val error = Exception("Phone update error")
        linkAccountManager.updatePhoneNumberResult = Result.failure(error)

        val result = interactor.updatePhoneNumber("+1234567890")
        assertThat(result).isInstanceOf(LinkController.UpdatePhoneNumberResult.Failed::class.java)
        assertThat((result as LinkController.UpdatePhoneNumberResult.Failed).error).isEqualTo(error)
    }

    @Test
    fun `onPresentPaymentMethods() launches Link with correct arguments`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher, "test@example.com", null)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode)
            .isEqualTo(
                LinkLaunchMode.PaymentMethodSelection(
                    selectedPayment = null,
                    sharePaymentDetailsImmediatelyAfterCreation = false,
                    shouldShowSecondaryCta = false,
                )
            )

        val state = interactor.state(application).first()
        assertThat(state.isConsumerVerified).isNull()
    }

    @Test
    fun `onPresentPaymentMethods() on matching email passes existing account`() = runTest(dispatcher) {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher, TestFactory.LINK_ACCOUNT.email, null)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isEqualTo(TestFactory.LINK_ACCOUNT)
    }

    @Test
    fun `onPresentPaymentMethods() on non-matching email clears account state`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        interactor.updateState {
            it.copy(
                selectedPaymentMethod = createTestPaymentMethod(),
                createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        }

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher, "another@email.com", null)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isNull()

        interactor.state(application).test {
            val state = awaitItem()
            assertThat(state.selectedPaymentMethodPreview).isNull()
            assertThat(state.createdPaymentMethod).isNull()
        }
    }

    @Test
    fun `onPresentPaymentMethods() with email includes the email in the launch config`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val newEmail = "new@email.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher = launcher, email = newEmail, paymentMethodType = null)

        val customerInfo = launcher.calls.awaitItem().input.configuration.customerInfo
        assertThat(customerInfo.email).isEqualTo(newEmail)
    }

    @Test
    fun `onPresentPaymentMethods() with null email uses existing customer info`() = runTest {
        val interactor = createInteractor()
        val billingDetails = PaymentSheet.BillingDetails(
            address = null,
            email = TestFactory.CUSTOMER_EMAIL,
            name = TestFactory.CUSTOMER_NAME,
            phone = TestFactory.CUSTOMER_PHONE,
        )
        configure(interactor, defaultBillingDetails = Optional.of(billingDetails))

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher = launcher, email = null, paymentMethodType = null)

        val customerInfo = launcher.calls.awaitItem().input.configuration.customerInfo
        assertThat(customerInfo.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(customerInfo.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(customerInfo.phone).isEqualTo(TestFactory.CUSTOMER_PHONE)
    }

    @Test
    fun `onLinkActivityResult() with PaymentMethodObtained result does nothing`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val initialState = interactor.state(application).first()
        val initialAccount = linkAccountHolder.linkAccountInfo.first()

        // First call onPresentPaymentMethods to set up the launch mode
        interactor.presentPaymentMethods(FakeActivityResultLauncher(), "test@example.com", null)

        interactor.presentPaymentMethodsResultFlow.test {
            interactor.onLinkActivityResult(
                LinkActivityResult.PaymentMethodObtained(
                    paymentMethod = mock()
                )
            )
            expectNoEvents()
        }

        assertThat(interactor.state(application).first()).isEqualTo(initialState)
        assertThat(linkAccountHolder.linkAccountInfo.first()).isEqualTo(initialAccount)
    }

    @Test
    fun `onLinkActivityResult() with Canceled result`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        // First call onPresentPaymentMethods to set up the launch mode
        interactor.presentPaymentMethods(FakeActivityResultLauncher(), "test@example.com", null)

        interactor.presentPaymentMethodsResultFlow.test {
            interactor.onLinkActivityResult(
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
        val interactor = createInteractor()
        configure(interactor)

        // First call onPresentPaymentMethods to set up the launch mode
        interactor.presentPaymentMethods(FakeActivityResultLauncher(), "test@example.com", null)

        val linkPaymentMethod = createTestPaymentMethod()
        interactor.presentPaymentMethodsResultFlow.test {
            interactor.onLinkActivityResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                    selectedPayment = linkPaymentMethod,
                    shippingAddress = null,
                )
            )

            assertThat(awaitItem()).isEqualTo(LinkController.PresentPaymentMethodsResult.Success)
        }

        interactor.state(application).test {
            assertThat(awaitItem().selectedPaymentMethodPreview?.sublabel)
                .isEqualTo("Visa Credit •••• 4242")
        }
    }

    @Test
    fun `onLinkActivityResult() with Failed result`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        // First call onPresentPaymentMethods to set up the launch mode
        interactor.presentPaymentMethods(FakeActivityResultLauncher(), "test@example.com", null)

        val error = Exception("Error")
        interactor.presentPaymentMethodsResultFlow.test {
            interactor.onLinkActivityResult(
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
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        interactor.onLinkActivityResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )

        interactor.state(application).test {
            assertThat(awaitItem().isConsumerVerified).isNull()
        }
    }

    @Test
    fun `onAuthenticate() fails when configuration is not set`() = runTest {
        val interactor = createInteractor()

        interactor.authenticationResultFlow.test {
            interactor.authenticate(mock(), "test@example.com")
            val result = awaitItem() as LinkController.AuthenticationResult.Failed
            assertThat(result.error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onAuthenticate() launches Link with correct arguments`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, "test@example.com")

        val args = launcher.calls.awaitItem().input
        assertThat(args.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())

        val state = interactor.state(application).first()
        assertThat(state.isConsumerVerified).isNull()
    }

    @Test
    fun `onAuthenticate() on matching email passes existing account`() = runTest(dispatcher) {
        val interactor = createInteractor()
        configure(interactor)

        // Create an unverified account (one that needs verification)
        val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
        )
        val unverifiedAccount = LinkAccount(unverifiedSession)
        linkAccountHolder.set(LinkAccountUpdate.Value(unverifiedAccount))

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, unverifiedAccount.email)

        val args = launcher.calls.awaitItem().input
        assertThat(args.linkAccountInfo.account).isEqualTo(unverifiedAccount)
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() on non-matching email clears account state`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, "another@email.com")

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() updates state with presentedForEmail and currentLaunchMode`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val email = "test@example.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, email)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.configuration.customerInfo.email).isEqualTo(email)
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() handles configuration loading failure`() = runTest {
        val interactor = createInteractor()
        // Don't configure the interactor, so it will fail with MissingConfigurationException

        interactor.authenticationResultFlow.test {
            interactor.authenticate(mock(), "test@example.com")

            val result = awaitItem() as LinkController.AuthenticationResult.Failed
            assertThat(result.error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Test
    fun `onAuthenticate() with null email passes null to configuration`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, null)

        val call = launcher.calls.awaitItem()
        val args = call.input
        assertThat(args.configuration.customerInfo.email).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication())
    }

    @Test
    fun `onAuthenticate() sets correct customer info in configuration`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val email = "test@example.com"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, email)

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
        val interactor = createInteractor()
        configure(interactor)

        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticateExistingConsumer(launcher, "test@example.com")

        val args = launcher.calls.awaitItem().input
        assertThat(args.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authentication(existingOnly = true))
    }

    @Test
    fun `onAuthenticateExistingConsumer() on succeeds immediately with matching email`() = runTest(dispatcher) {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        interactor.authenticationResultFlow.test {
            val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
            interactor.authenticateExistingConsumer(launcher, TestFactory.LINK_ACCOUNT.email)

            assertThat(awaitItem()).isEqualTo(LinkController.AuthenticationResult.Success)
        }
    }

    @Test
    fun `getLinkAccountInfo() clears account holder when account is null`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        // Set up an existing account
        signIn()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(TestFactory.LINK_ACCOUNT)

        // Call onAuthenticate with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, "different@email.com")

        // Verify that the account holder was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()

        // Verify that the launcher was called with null account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isNull()
    }

    @Test
    fun `getLinkAccountInfo() does not clear account holder when account is not null`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        // Create an unverified account (one that needs verification)
        val unverifiedSession = TestFactory.CONSUMER_SESSION.copy(
            verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
        )
        val unverifiedAccount = LinkAccount(unverifiedSession)
        linkAccountHolder.set(LinkAccountUpdate.Value(unverifiedAccount))
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(unverifiedAccount)

        // Call onAuthenticate with a matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authenticate(launcher, unverifiedAccount.email)

        // Verify that the account holder was NOT cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(unverifiedAccount)

        // Verify that the launcher was called with the existing account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isEqualTo(unverifiedAccount)
    }

    @Test
    fun `onAuthenticate() on non-matching email clears saved payment data`() = runTest {
        testAuthenticationClearsSavedPaymentData { interactor, launcher, email ->
            interactor.authenticate(launcher, email)
        }
    }

    @Test
    fun `onAuthenticateExistingConsumer() on non-matching email clears saved payment data`() = runTest {
        testAuthenticationClearsSavedPaymentData { interactor, launcher, email ->
            interactor.authenticateExistingConsumer(launcher, email)
        }
    }

    @Test
    fun `onPresentPaymentMethods() clears account holder when email doesn't match`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        // Set up an existing account
        signIn()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(TestFactory.LINK_ACCOUNT)

        // Call onPresentPaymentMethods with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.presentPaymentMethods(launcher, "different@email.com", null)

        // Verify that the account holder was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()

        // Verify that the launcher was called with null account
        val call = launcher.calls.awaitItem()
        assertThat(call.input.linkAccountInfo.account).isNull()
    }

    @Test
    fun `onRegisterConsumer() on success emits success result and updates account`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val expectedAccount = TestFactory.LINK_ACCOUNT
        linkAccountManager.signupResult = Result.success(expectedAccount)

        assertThat(interactor.registerConsumerWith())
            .isEqualTo(LinkController.RegisterConsumerResult.Success)

        // Verify that the account was updated
        val account = linkAccountHolder.linkAccountInfo.value.account
        assertThat(account).isEqualTo(expectedAccount)
    }

    @Test
    fun `onRegisterConsumer() on failure emits failure result and clears account`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val error = Exception("Registration failed")
        linkAccountManager.signupResult = Result.failure(error)

        val result = interactor.registerConsumerWith()

        assertThat(result).isInstanceOf(LinkController.RegisterConsumerResult.Failed::class.java)
        assertThat((result as LinkController.RegisterConsumerResult.Failed).error).isEqualTo(error)

        // Verify that the account was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    @Test
    fun `onRegisterConsumer() on attestation failure emits failure result with AppAttestationException`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val attestationError = Exception("Attestation failed")
        linkAccountManager.signupResult = Result.failure(AppAttestationException(attestationError))

        val result = interactor.registerConsumerWith()

        assertThat(result).isInstanceOf(LinkController.RegisterConsumerResult.Failed::class.java)
        val failedResult = result as LinkController.RegisterConsumerResult.Failed
        assertThat(failedResult.error).isInstanceOf(AppAttestationException::class.java)
        assertThat(failedResult.error.cause).isEqualTo(attestationError)

        // Verify that the account was cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    private suspend fun testAuthenticationClearsSavedPaymentData(
        authenticateCall: (
            interactor: LinkControllerInteractor,
            launcher: FakeActivityResultLauncher<LinkActivityContract.Args>,
            email: String
        ) -> Unit
    ) {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        // Set up some saved payment data
        val selectedPaymentMethod = createTestPaymentMethod()
        val createdPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        interactor.updateState {
            it.copy(
                selectedPaymentMethod = selectedPaymentMethod,
                createdPaymentMethod = createdPaymentMethod
            )
        }

        // Verify initial state has the saved data
        interactor.state(application).test {
            val initialState = awaitItem()
            assertThat(initialState.selectedPaymentMethodPreview).isNotNull()
            assertThat(initialState.createdPaymentMethod).isEqualTo(createdPaymentMethod)
        }

        // Call the authentication method with a non-matching email
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        authenticateCall(interactor, launcher, "another@email.com")

        // Verify that saved payment data is cleared
        interactor.state(application).test {
            val finalState = awaitItem()
            assertThat(finalState.selectedPaymentMethodPreview).isNull()
            assertThat(finalState.createdPaymentMethod).isNull()
        }
    }

    private fun createInteractor(): LinkControllerInteractor {
        return LinkControllerInteractor(
            application = application,
            logger = logger,
            linkConfigurationLoader = linkConfigurationLoader,
            linkAccountHolder = linkAccountHolder,
            linkComponentBuilderProvider = linkComponentBuilderProvider,
        )
    }

    private suspend fun configure(
        interactor: LinkControllerInteractor,
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
        interactor.configure(
            LinkController.Configuration.Builder("Test", ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
                .apply { defaultBillingDetails?.let { defaultBillingDetails(it.getOrNull()) } }
                .apply { billingDetailsCollectionConfiguration?.let { billingDetailsCollectionConfiguration(it) } }
                .build()
        )
    }

    private fun createControllerConfig() =
        LinkController.Configuration.default(
            context = application,
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )

    private fun signIn() {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
    }

    private fun createTestPaymentMethod(
        cvc: String = "123",
        billingPhone: String? = null
    ): LinkPaymentMethod.ConsumerPaymentDetails {
        return LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = cvc,
            billingPhone = billingPhone
        )
    }

    private suspend fun configureWithAttestation(
        interactor: LinkControllerInteractor,
        attestationResult: LinkAttestationCheck.Result = LinkAttestationCheck.Result.Successful
    ) {
        linkAttestationCheck.result = attestationResult
        val loadedConfiguration = LinkTestUtils.createLinkConfiguration()
        linkConfigurationLoader.linkConfigurationResult = Result.success(loadedConfiguration)
        interactor.configure(createControllerConfig())
    }

    @Test
    fun `authorize() launches Link with correct arguments`() = runTest {
        val interactor = createInteractor()
        configure(interactor)

        val linkAuthIntentId = "lai_test123"
        val launcher = FakeActivityResultLauncher<LinkActivityContract.Args>()
        interactor.authorize(launcher, linkAuthIntentId)

        val args = launcher.calls.awaitItem().input
        assertThat(args.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(args.linkAccountInfo.account).isNull()
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.Authorization(linkAuthIntentId = linkAuthIntentId))
    }

    @Test
    fun `authorize() fails when configuration is not set`() = runTest {
        val interactor = createInteractor()

        interactor.authorizeResultFlow.test {
            interactor.authorize(mock(), "lai_test123")
            val result = awaitItem() as LinkController.AuthorizeResult.Failed
            assertThat(result.error).isInstanceOf(MissingConfigurationException::class.java)
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `onLinkActivityResult() with Authorization results`() = runTest {
        data class AuthorizationTestCase(
            val name: String,
            val linkActivityResult: LinkActivityResult,
            val expectedAuthorizeResult: LinkController.AuthorizeResult
        )

        val error = Exception("Authorization error")
        val testCases = listOf(
            AuthorizationTestCase(
                name = "Canceled",
                linkActivityResult = LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                ),
                expectedAuthorizeResult = LinkController.AuthorizeResult.Canceled
            ),
            AuthorizationTestCase(
                name = "Completed - Consented",
                linkActivityResult = LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(null),
                    selectedPayment = null,
                    shippingAddress = null,
                    authorizationConsentGranted = true
                ),
                expectedAuthorizeResult = LinkController.AuthorizeResult.Consented
            ),
            AuthorizationTestCase(
                name = "Completed - Denied",
                linkActivityResult = LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(null),
                    selectedPayment = null,
                    shippingAddress = null,
                    authorizationConsentGranted = false
                ),
                expectedAuthorizeResult = LinkController.AuthorizeResult.Denied
            ),
            AuthorizationTestCase(
                name = "Completed - null consent (fallback to Canceled)",
                linkActivityResult = LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(null),
                    selectedPayment = null,
                    shippingAddress = null,
                    authorizationConsentGranted = null
                ),
                expectedAuthorizeResult = LinkController.AuthorizeResult.Canceled
            ),
            AuthorizationTestCase(
                name = "Failed",
                linkActivityResult = LinkActivityResult.Failed(
                    error = error,
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                ),
                expectedAuthorizeResult = LinkController.AuthorizeResult.Failed(error)
            )
        )

        testCases.forEach { testCase ->
            val interactor = createInteractor()
            configure(interactor)

            // Set up the launch mode
            interactor.authorize(FakeActivityResultLauncher(), "lai_test123")

            interactor.authorizeResultFlow.test {
                interactor.onLinkActivityResult(testCase.linkActivityResult)
                assertThat(awaitItem()).isEqualTo(testCase.expectedAuthorizeResult)
            }
        }
    }

    @Test
    fun `logOut() succeeds and clears account`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val result = interactor.logOut()

        assertThat(result).isInstanceOf(LinkController.LogOutResult.Success::class.java)
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    @Test
    fun `clearLinkAccount() clears account from holder`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        // Verify account is initially present
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNotNull()

        // Clear the account
        interactor.clearLinkAccount()

        // Verify account is cleared
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    @Test
    fun `logOut() succeeds and clears account even when request fails`() = runTest {
        val interactor = createInteractor()
        configure(interactor)
        signIn()

        val error = Exception("Logout failed")
        linkAccountManager.logOutResult = Result.failure(error)

        val result = interactor.logOut()

        assertThat(result).isInstanceOf(LinkController.LogOutResult.Success::class.java)
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
    }

    @Test
    fun `logOut() fails when configuration is not set`() = runTest {
        val interactor = createInteractor()

        val result = interactor.logOut()

        assertThat(result).isInstanceOf(LinkController.LogOutResult.Failed::class.java)
        val error = (result as LinkController.LogOutResult.Failed).error
        assertThat(error).isInstanceOf(MissingConfigurationException::class.java)
    }

    private data class ConsumerRegistrationParams(
        val email: String = "test@example.com",
        val phone: String = "1234567890",
        val country: String = "US",
        val name: String = "Test User"
    )

    private suspend fun LinkControllerInteractor.registerConsumerWith(
        params: ConsumerRegistrationParams = ConsumerRegistrationParams()
    ) = registerConsumer(
        email = params.email,
        phone = params.phone,
        country = params.country,
        name = params.name
    )
}
