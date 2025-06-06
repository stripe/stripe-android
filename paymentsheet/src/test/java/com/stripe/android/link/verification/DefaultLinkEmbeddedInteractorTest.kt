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
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DefaultLinkEmbeddedInteractorTest {
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

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Resolved)
    }

    @Test
    fun `when account status is LoggedIn, should mark as Resolved`() = runTest {
        // Setup
        linkAccountManager.setLinkAccount(
            LinkAccountUpdate.Value(createLinkAccount(AccountStatus.Verified))
        )
        val metadata = createPaymentMethodMetadata()
        val manager = createManager()

        manager.setup(
            paymentMethodMetadata = metadata
        )

        testScope.advanceUntilIdle()

        assertThat(manager.state.value.verificationState).isEqualTo(VerificationState.Resolved)
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

        assertThat(manager.state.value.verificationState).isInstanceOf(VerificationState.Verifying::class.java)
    }

    private fun createManager(): LinkEmbeddedInteractor {
        return DefaultLinkEmbeddedInteractor(
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
}
