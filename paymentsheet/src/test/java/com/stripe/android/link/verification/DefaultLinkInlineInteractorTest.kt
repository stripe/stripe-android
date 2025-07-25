package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.verification.VerificationState.Loading
import com.stripe.android.link.verification.VerificationState.Render2FA
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.FakeLinkComponent
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLinkInlineInteractorTest {
    private val savedStateHandle = SavedStateHandle()
    private val linkLauncher = mock<LinkPaymentLauncher>()
    private val linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager()
    private val linkGate: FakeLinkGate = FakeLinkGate()
    private val component = FakeLinkComponent(
        linkAccountManager = linkAccountManager,
        linkGate = linkGate
    )
    private val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
        component = component,
        linkGate = linkGate
    )

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val rule: TestRule = CoroutineTestRule(testDispatcher)

    init {
        linkGate.setUseNativeLink(true)
    }

    @Test
    fun `initial state should be Loading`() = runTest(testDispatcher) {
        val interactor = createInteractor()

        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(Loading)
        }
    }

    @Test
    fun `when setup is called with link disabled, should render button`() = runTest(testDispatcher) {
        val metadata = createPaymentMethodMetadata(linkState = null)
        val interactor = createInteractor()

        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(Loading)

            interactor.setup(paymentMethodMetadata = metadata)

            assertThat(awaitItem().verificationState).isEqualTo(VerificationState.RenderButton)
        }
    }

    @Test
    fun `when killswitch is enabled, should render button regardless of account status`() = runTest(testDispatcher) {
        // Setup account that would normally need verification
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.NeedsVerification))
        )

        // Create configuration with killswitch enabled
        val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
            linkMobileExpressCheckoutElementInlineOtpKillswitch = true
        )

        val metadata = createPaymentMethodMetadata(
            linkState = LinkState(
                loginState = LoginState.NeedsVerification,
                configuration = linkConfiguration,
                signupMode = null
            )
        )
        val interactor = createInteractor()

        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(Loading)

            interactor.setup(paymentMethodMetadata = metadata)

            // Should render button instead of starting verification, even though account needs verification
            assertThat(awaitItem().verificationState).isEqualTo(VerificationState.RenderButton)
        }

        // Verify that startVerification was NOT called
        linkAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `when account status is LoggedIn, should render button`() = runTest(testDispatcher) {
        // Setup
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.Verified))
        )
        val metadata = createPaymentMethodMetadata()
        val interactor = createInteractor()

        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(Loading)

            interactor.setup(paymentMethodMetadata = metadata)

            assertThat(awaitItem().verificationState).isEqualTo(VerificationState.RenderButton)
        }
    }

    @Test
    fun `when account status is NeedsVerification, should call startVerification`() = runTest(testDispatcher) {
        // Setup
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.NeedsVerification))
        )
        val metadata = createPaymentMethodMetadata()

        // Execute
        val interactor = createInteractor()

        // Verify
        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(Loading)

            interactor.setup(paymentMethodMetadata = metadata)

            linkAccountManager.awaitStartVerificationCall()

            assertThat(awaitItem().verificationState).isInstanceOf(Render2FA::class.java)
        }
    }

    @Test
    fun `when otp complete and confirmation succeeds, keeps status as Render2FA and launches Link`() =
        runTest(testDispatcher) {
            // Setup
            val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
            linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
            linkAccountManager.confirmVerificationResult = Result.success(mockAccount)

            val interactor = createInteractor()

            interactor.setup(createPaymentMethodMetadata())

            // Submit OTP
            val otpController = interactor.otpElement.controller
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            interactor.state.test {
                assertThat(awaitItem().verificationState).isInstanceOf(Render2FA::class.java)
            }
            verify(linkLauncher).present(
                configuration = any(),
                linkAccountInfo = any(),
                launchMode = eq(LinkLaunchMode.PaymentMethodSelection(null)),
                useLinkExpress = any()
            )
        }

    @Test
    fun `when verification state is not Render2FA, otp complete should be ignored`() = runTest(testDispatcher) {
        // Setup
        val otpCompleteFlow = MutableSharedFlow<String>()

        val interactor = createInteractor()

        // Setup state as RenderButton
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = VerificationState.RenderButton
        )

        interactor.setup(createPaymentMethodMetadata())

        // Submit OTP
        otpCompleteFlow.emit("123456")

        // State should remain unchanged
        interactor.state.test {
            assertThat(awaitItem().verificationState).isEqualTo(VerificationState.RenderButton)
        }
    }

    @Test
    fun `when verification is already processing, otp complete should be ignored`() = runTest(testDispatcher) {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))

        val interactor = createInteractor()

        // Setup initial state with isProcessing=true
        val initialViewState = VerificationViewState(
            isProcessing = true, // Already processing
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        interactor.setup(createPaymentMethodMetadata())

        // Make sure the confirm verification doesn't get called
        linkAccountManager.confirmVerificationResult = Result.success(mockAccount)

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Submit OTP
        val otpController = interactor.otpElement.controller
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }

        // Verify that confirmation wasn't called
        linkAccountManager.confirmVerificationTurbine.expectNoEvents()
    }

    @Test
    fun `onConfirmationResult should set error state on failure`() = runTest(testDispatcher) {
        // Setup
        val interactor = createInteractor()
        val testError = RuntimeException("Invalid OTP code")

        val initialViewState = VerificationViewState(
            isProcessing = true,
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Execute
        interactor.onConfirmationResult(
            verificationState = verificationState,
            result = Result.failure(testError)
        )

        // Verify
        interactor.state.test {
            val state = awaitItem()
            val resultState = state.verificationState as Render2FA
            assertThat(resultState.viewState.isProcessing).isFalse()
            assertThat(resultState.viewState.errorMessage).isNotNull()
        }
    }

    @Test
    fun `resendCode should reset OTP controller and start verification`() = runTest(testDispatcher) {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
        linkAccountManager.startVerificationResult = Result.success(createLinkAccount())

        val interactor = createInteractor()

        // Setup initial state with some OTP values and error
        val initialViewState = VerificationViewState(
            isProcessing = false,
            errorMessage = "Previous error".resolvableString,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Fill OTP with some values
        val otpController = interactor.otpElement.controller
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }

        // Execute
        interactor.resendCode()

        // Verify
        linkAccountManager.awaitStartVerificationCall()

        interactor.state.test {
            val state = awaitItem()
            val resultState = state.verificationState as Render2FA
            assertThat(resultState.viewState.isSendingNewCode).isFalse()
            assertThat(resultState.viewState.didSendNewCode).isTrue()
            assertThat(resultState.viewState.errorMessage).isNull()
        }

        // Verify OTP was reset
        assertThat(otpController.fieldValue.value).isEmpty()
    }

    @Test
    fun `resendCode should handle verification failure`() = runTest(testDispatcher) {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
        val testError = RuntimeException("Verification failed")
        linkAccountManager.startVerificationResult = Result.failure(testError)

        val interactor = createInteractor()

        val initialViewState = VerificationViewState(
            isProcessing = false,
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Execute
        interactor.resendCode()

        // Verify
        linkAccountManager.awaitStartVerificationCall()

        interactor.state.test {
            val state = awaitItem()
            val resultState = state.verificationState as Render2FA
            assertThat(resultState.viewState.isSendingNewCode).isFalse()
            assertThat(resultState.viewState.didSendNewCode).isFalse()
            assertThat(resultState.viewState.errorMessage).isNotNull()
        }
    }

    @Test
    fun `resendCode should clear error message`() = runTest(testDispatcher) {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
        linkAccountManager.startVerificationResult = Result.success(createLinkAccount())

        val interactor = createInteractor()

        val initialViewState = VerificationViewState(
            isProcessing = false,
            errorMessage = "Previous error message".resolvableString,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Execute
        interactor.resendCode()

        // Verify
        linkAccountManager.awaitStartVerificationCall()

        interactor.state.test {
            val state = awaitItem()
            val resultState = state.verificationState as Render2FA
            assertThat(resultState.viewState.errorMessage).isNull()
        }
    }

    @Test
    fun `resendCode should reset OTP controller even when verification fails`() = runTest(testDispatcher) {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
        val testError = RuntimeException("Verification failed")
        linkAccountManager.startVerificationResult = Result.failure(testError)

        val interactor = createInteractor()

        val initialViewState = VerificationViewState(
            isProcessing = false,
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            defaultPayment = null,
            didSendNewCode = false
        )

        val verificationState = Render2FA(
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            viewState = initialViewState
        )

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = verificationState
        )

        // Fill OTP with some values
        val otpController = interactor.otpElement.controller
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }

        // Execute
        interactor.resendCode()

        // Verify
        linkAccountManager.awaitStartVerificationCall()

        // Verify OTP was reset even though verification failed
        assertThat(otpController.fieldValue.value).isEmpty()
    }

    private fun createInteractor() = DefaultLinkInlineInteractor(
        coroutineScope = TestScope(testDispatcher),
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        linkLauncher = linkLauncher,
        savedStateHandle = savedStateHandle,
        logger = FakeLogger()
    )

    private fun createPaymentMethodMetadata(
        linkState: LinkState? = LinkState(
            loginState = LoginState.NeedsVerification,
            configuration = createLinkConfiguration(),
            signupMode = null
        ),
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            linkState = linkState
        )
    }

    private fun createLinkAccount(
        accountStatus: AccountStatus = AccountStatus.NeedsVerification,
        redactedPhoneNumber: String = "",
        email: String = ""
    ): LinkAccount {
        val mockAccount = mock<LinkAccount>()
        whenever(mockAccount.accountStatus).thenReturn(accountStatus)
        whenever(mockAccount.redactedPhoneNumber).thenReturn(redactedPhoneNumber)
        whenever(mockAccount.email).thenReturn(email)
        return mockAccount
    }

    companion object {
        private const val LINK_EMBEDDED_STATE_KEY = "LINK_EMBEDDED_STATE_KEY"
    }
}
