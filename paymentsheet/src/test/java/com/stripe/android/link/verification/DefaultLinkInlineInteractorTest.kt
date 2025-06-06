package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLinkInlineInteractorTest {
    private val savedStateHandle = SavedStateHandle()
    private val linkAccountManager = FakeLinkAccountManager()
    private val mockComponent = mock<LinkComponent> {
        on { linkAccountManager } doReturn linkAccountManager
    }
    private val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator> {
        on { getComponent(any()) } doReturn mockComponent
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val prominenceFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.showInlineSignupInWalletButtons,
        isEnabled = true
    )

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() {
        val manager = createManager()
        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Loading)
    }

    @Test
    fun `when setup is called with link disabled, should mark as resolved`() = runTest {
        val metadata = createPaymentMethodMetadata(linkState = null)
        val manager = createManager()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Loading)

        manager.setup(
            paymentMethodMetadata = metadata
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.RenderButton)
    }

    @Test
    fun `when account status is LoggedIn, should mark as Resolved`() = runTest {
        // Setup
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.Verified))
        )
        val metadata = createPaymentMethodMetadata()
        val manager = createManager()
        manager.otpElement.controller

        manager.setup(
            paymentMethodMetadata = metadata
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.RenderButton)
    }

    @Test
    fun `when account status is NeedsVerification, should call startVerification`() = runTest {
        // Setup
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.NeedsVerification))
        )
        val metadata = createPaymentMethodMetadata()

        // Execute
        val manager = createManager()

        manager.setup(
            paymentMethodMetadata = metadata
        )

        testScope.advanceUntilIdle()

        // Verify
        linkAccountManager.startVerificationResult
        linkAccountManager.awaitStartVerificationCall()

        assertThat(manager.state.value.verificationState).isInstanceOf(VerificationState.Render2FA::class.java)
    }

    @Test
    fun `when otp is complete and confirmation successful, should update to RenderButton state`() = runTest {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))
        linkAccountManager.confirmVerificationResult = Result.success(mockAccount)

        val manager = createManager()

        manager.setup(createPaymentMethodMetadata())

        testScope.advanceUntilIdle()

        // Submit OTP
        val otpController = manager.otpElement.controller
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }
        testScope.advanceUntilIdle()

        // Verify
        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.RenderButton)
    }

    @Test
    fun `when verification state is not Render2FA, otp complete should be ignored`() = runTest {
        // Setup
        val otpCompleteFlow = MutableSharedFlow<String>()

        val manager = createManager()

        // Setup state as RenderButton
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = VerificationState.RenderButton
        )

        manager.setup(createPaymentMethodMetadata())
        testScope.advanceUntilIdle()

        // Submit OTP
        otpCompleteFlow.emit("123456")
        testScope.advanceUntilIdle()

        // State should remain unchanged
        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.RenderButton)
    }

    @Test
    fun `when verification is already processing, otp complete should be ignored`() = runTest {
        // Setup
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(mockAccount))

        val manager = createManager()

        // Setup initial state with isProcessing=true
        val initialViewState = VerificationViewState(
            isProcessing = true, // Already processing
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            didSendNewCode = false
        )

        manager.setup(createPaymentMethodMetadata())
        testScope.advanceUntilIdle()

        // Make sure the confirm verification doesn't get called
        linkAccountManager.confirmVerificationResult = Result.success(mockAccount)

        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = VerificationState.Render2FA(initialViewState)
        )

        // Submit OTP
        val otpController = manager.otpElement.controller
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }
        testScope.advanceUntilIdle()

        // Verify that confirmation wasn't called
        linkAccountManager.confirmVerificationTurbine.expectNoEvents()
    }

    @Test
    fun `onConfirmationResult should set state to RenderButton on success`() = runTest {
        // Setup
        val manager = createManager()
        val mockAccount = createLinkAccount(AccountStatus.NeedsVerification)
        
        val initialViewState = VerificationViewState(
            isProcessing = true,
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            didSendNewCode = false
        )
        
        val render2FA = VerificationState.Render2FA(initialViewState)
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = render2FA
        )
        
        // Execute
        manager.onConfirmationResult(render2FA, Result.success(mockAccount))
        testScope.advanceUntilIdle()
        
        // Verify
        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.RenderButton)
    }
    
    @Test
    fun `onConfirmationResult should set error state on failure`() = runTest {
        // Setup
        val manager = createManager()
        val testError = RuntimeException("Invalid OTP code")
        
        val initialViewState = VerificationViewState(
            isProcessing = true,
            errorMessage = null,
            email = "test@example.com",
            redactedPhoneNumber = "****1234",
            requestFocus = false,
            isDialog = true,
            isSendingNewCode = false,
            didSendNewCode = false
        )
        
        val render2FA = VerificationState.Render2FA(initialViewState)
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = LinkInlineState(
            verificationState = render2FA
        )
        
        // Execute
        manager.onConfirmationResult(render2FA, Result.failure(testError))
        testScope.advanceUntilIdle()
        
        // Verify
        val verificationState = manager.state.value.verificationState as VerificationState.Render2FA
        assertThat(verificationState.viewState.isProcessing).isFalse()
        assertThat(verificationState.viewState.errorMessage).isNotNull()
    }

    private fun createManager(): DefaultLinkInlineInteractor {
        return DefaultLinkInlineInteractor(
            coroutineScope = testScope,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle
        )
    }

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