package com.stripe.android.link.verification

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LinkEmbeddedManagerTest {

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

    private val unconfinedDispatcher = UnconfinedTestDispatcher()

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
    fun `when setup is called with no LINK wallet type, should do nothing`() = runTest {
        val metadata = createPaymentMethodMetadata(hasLinkWallet = false)
        val manager = createManager()
        var verificationSucceeded = false

        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = { verificationSucceeded = true }
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Loading)
        assertThat(verificationSucceeded).isFalse()
    }

    @Test
    fun `when account status is LoggedOut, should mark as Resolved`() = runTest {
        val metadata = createPaymentMethodMetadata(
            loginState = LoginState.LoggedOut
        )
        val manager = createManager()

        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = {}
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Resolved)
    }

    @Test
    fun `when account status is Error, should mark as Resolved`() = runTest {
        val metadata = createPaymentMethodMetadata(
            loginState = LoginState.LoggedIn
        )
        val manager = createManager()

        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = {}
        )

        // Set account status to represent "null" scenario
        // Instead of passing null, we'll pass AccountStatus.Error which should be handled similarly
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.Error))
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Resolved)
    }

    @Test
    fun `when account status is Verified, should mark as Resolved`() = runTest {
        // Setup
        val metadata = createPaymentMethodMetadata(
            loginState = LoginState.LoggedIn
        )
        val manager = createManager()

        // Execute
        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = {}
        )

        // Set account status to Verified
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.Verified))
        )

        testScope.advanceUntilIdle()

        // Verify
        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Resolved)
    }

    @Test
    fun `when account status is NeedsVerification, should call startVerification`() = runTest {
        // Setup
        val metadata = createPaymentMethodMetadata(
            loginState = LoginState.NeedsVerification
        )
        val manager = createManager()

        // Execute
        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = {}
        )

        // Set account status to NeedsVerification
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.NeedsVerification))
        )

        testScope.advanceUntilIdle()

        // Verify
        linkAccountManager.startVerificationResult
        linkAccountManager.awaitStartVerificationCall()
    }

    @Test
    fun `when account status is VerificationStarted, should update to Verifying state`() = runTest {
        // Setup
        val metadata = createPaymentMethodMetadata(
            loginState = LoginState.NeedsVerification
        )
        val manager = createManager()

        // Execute
        manager.setup(
            paymentMethodMetadata = metadata,
            onVerificationSucceeded = {}
        )

        // Create test account with phone number and email
        val testAccount = createLinkAccount(
            AccountStatus.VerificationStarted,
            redactedPhoneNumber = "1234",
            email = "test@example.com"
        )

        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(testAccount)
        )

        testScope.advanceUntilIdle()

        // Verify
        val currentState = manager.state.value.verificationState
        val verifyingState = currentState as VerificationState.Verifying
        assertThat(verifyingState.viewState.redactedPhoneNumber).isEqualTo("1234")
        assertThat(verifyingState.viewState.email).isEqualTo("test@example.com")
        assertThat(verifyingState.viewState.isProcessing).isFalse()
    }

    @Test
    fun `createLinkSelection should return selection with preserved payment method`() = runTest {
        // Setup
        val manager = createManager()
        val testPaymentMethod = mock<LinkPaymentMethod>()

        // Set preserved payment method
        val currentState = manager.state.value
        savedStateHandle[LINK_EMBEDDED_STATE_KEY] = currentState.copy(
            preservedPaymentMethod = testPaymentMethod
        )

        // Execute
        val selection = manager.createLinkSelection()

        // Verify
        assertThat(selection).isInstanceOf(PaymentSelection.Link::class.java)
    }

    private fun createManager(): LinkEmbeddedManager {
        return LinkEmbeddedManager(
            coroutineScope = testScope,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle,
            logger = Logger.noop()
        )
    }

    private fun createPaymentMethodMetadata(
        hasLinkWallet: Boolean = true,
        loginState: LoginState = LoginState.LoggedIn
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            availableWallets = if (hasLinkWallet) listOf(WalletType.Link) else emptyList(),
            linkState = LinkState(
                loginState = loginState,
                configuration = createLinkConfiguration(),
                signupMode = null
            )
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