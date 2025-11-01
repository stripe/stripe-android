package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.LoggedOut
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAuthIntentInfo
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.wallet.AddPaymentMethodOptions
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.LinkAuthIntent
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.addresselement.AutocompleteActivityLauncher
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.uicore.navigation.NavBackStackEntryUpdate
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

@SuppressWarnings("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `test that cancel result is called on back pressed`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val navigationManager = TestNavigationManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(
            navigationManager = navigationManager,
            linkAccountManager = linkAccountManager,
        )

        vm.result.test {
            vm.handleViewAction(LinkAction.BackPressed)
            assertThat(awaitItem()).isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
                )
            )
        }
    }

    @Test
    fun `test that activity registerForActivityResult registers confirmation handler & autocomplete launcher`() =
        runTest(dispatcher) {
            TestAutocompleteLauncher.test {
                val confirmationHandler = FakeConfirmationHandler()
                val vm = createViewModel(
                    autocompleteLauncher = launcher,
                    confirmationHandler = confirmationHandler
                )

                val activityResultCaller = DummyActivityResultCaller.noOp()

                vm.registerForActivityResult(
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = object : LifecycleOwner {
                        override val lifecycle: Lifecycle = mock()
                    }
                )

                vm.unregisterActivity()

                val autocompleteRegisterCall = registerCalls.awaitItem()
                val confirmationHandlerRegisterCall = confirmationHandler.registerTurbine.awaitItem()

                assertThat(autocompleteRegisterCall.activityResultCaller).isEqualTo(activityResultCaller)
                assertThat(confirmationHandlerRegisterCall.activityResultCaller).isEqualTo(activityResultCaller)
            }
        }

    @Test
    fun `initializer throws exception when args are null`() {
        val savedStateHandle = SavedStateHandle()
            .apply {
                set(LinkActivity.EXTRA_ARGS, null)
            }

        val factory = LinkActivityViewModel.factory(savedStateHandle)

        assertFailsWith<NoArgsException> {
            factory.create(LinkActivityViewModel::class.java, creationExtras())
        }.also { exception ->
            assertThat(exception.message).isEqualTo("NativeLinkArgs not found")
        }
    }

    @Test
    fun `initializer creates ViewModel when args are valid`() {
        val mockArgs = NativeLinkArgs(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            requestSurface = RequestSurface.PaymentElement,
            publishableKey = "pk_123",
            stripeAccountId = null,
            linkExpressMode = LinkExpressMode.DISABLED,
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = null
            ),
            paymentElementCallbackIdentifier = "LinkNativeTestIdentifier",
            launchMode = LinkLaunchMode.Full,
        )
        val savedStateHandle = SavedStateHandle()
        val factory = LinkActivityViewModel.factory(savedStateHandle)
        savedStateHandle[LinkActivity.EXTRA_ARGS] = mockArgs

        val viewModel = factory.create(LinkActivityViewModel::class.java, creationExtras())
        assertThat(viewModel.activityRetainedComponent.configuration).isEqualTo(mockArgs.configuration)
    }

    @Test
    fun `linkAccount value returns latest value from link account manager`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val vm = createViewModel(linkAccountManager = linkAccountManager)

        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        assertThat(vm.linkAccount).isEqualTo(TestFactory.LINK_ACCOUNT)
    }

    @Test
    fun `navigate should call NavigationManager with correct route and clear stack`() {
        val navigationManager = TestNavigationManager()

        val vm = createViewModel(
            navigationManager = navigationManager
        )

        vm.navigate(LinkScreen.SignUp, clearStack = true)

        navigationManager.assertNavigatedTo(
            route = LinkScreen.SignUp.route,
            popUpTo = PopUpToBehavior.Start,
        )
    }

    @Test
    fun `navigate should call NavigationManager with correct route and not clear stack`() {
        val navigationManager = TestNavigationManager()

        val vm = createViewModel(
            navigationManager = navigationManager
        )

        vm.navigate(LinkScreen.Wallet, clearStack = false)

        navigationManager.assertNavigatedTo(
            route = LinkScreen.Wallet.route,
            popUpTo = null,
        )
    }

    @Test
    fun `onCreate does not confirm when paymentReadyForConfirmation returns null`() = runTest {
        val vm = createViewModel(linkLaunchMode = LinkLaunchMode.Full)
        vm.onCreate(mock())
        advanceUntilIdle()
        // Should not emit Completed result, should proceed to attestation check and update screen state
        assertThat(vm.linkScreenState.value).isInstanceOf(ScreenState.FullScreen::class.java)
    }

    @Test
    fun `onCreate does not confirm when linkAccount is null`() = runTest {
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null
        )

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(null))

        val vm = createViewModel(
            linkLaunchMode = LinkLaunchMode.Confirmation(selectedPayment = selectedPayment),
            linkAccountManager = linkAccountManager
        )

        advanceUntilIdle()

        vm.result.test {
            vm.onCreate(mock())
            assertThat(awaitItem()).isInstanceOf(LinkActivityResult.Failed::class.java)
        }
    }

    @Test
    fun `onCreate confirms preselected Link payment when provided and emits Completed`() = runTest {
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null
        )

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val confirmationHandler = FakeLinkConfirmationHandler()
        confirmationHandler.confirmWithLinkPaymentDetailsResult = LinkConfirmationResult.Succeeded

        val vm = createViewModel(
            linkLaunchMode = LinkLaunchMode.Confirmation(selectedPayment = selectedPayment),
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = confirmationHandler
        )

        vm.result.test {
            vm.onCreate(mock())
            assertThat(awaitItem()).isInstanceOf(LinkActivityResult.Completed::class.java)
        }
    }

    @Test
    fun `onCreate does not emit Completed when confirmation is failed`() = runTest {
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null
        )

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setAccountStatus(AccountStatus.SignedOut)

        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        linkConfirmationHandler.confirmWithLinkPaymentDetailsResult =
            LinkConfirmationResult.Failed("something went wrong".resolvableString)
        linkConfirmationHandler.confirmResult = LinkConfirmationResult.Failed("something went wrong".resolvableString)

        val vm = createViewModel(
            linkLaunchMode = LinkLaunchMode.Confirmation(selectedPayment = selectedPayment),
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler
        )

        advanceUntilIdle()

        vm.result.test {
            vm.onCreate(mock())
            assertThat(awaitItem()).isInstanceOf(LinkActivityResult.Failed::class.java)
        }
    }

    @Test
    fun `onCreate should start with Wallet screen account status is Verified`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setAccountStatus(AccountStatus.Verified(true, null))

        val vm = createViewModel(linkAccountManager = linkAccountManager)

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.Wallet)
    }

    @Test
    fun `onCreate should start with verification screen when account status is NeedsVerification`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.Verification)
    }

    @Test
    fun `onCreate should start with verification screen when account status is VerificationStarted`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.VerificationStarted)

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.Verification)
    }

    @Test
    fun `onCreate should navigate to signUp screen when account status is SignedOut`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.SignedOut)

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.SignUp)
    }

    @Test
    fun `onCreate returns Failed when Authentication existingOnly and SignedOut`() = runTest {
        testAuthenticationFailureCase(
            accountStatus = AccountStatus.SignedOut,
            existingOnly = true,
            allowUserEmailEdits = true
        )
    }

    @Test
    fun `onCreate returns Failed result when Authentication existingOnly and account status is Error`() = runTest {
        testAuthenticationFailureCase(
            accountStatus = AccountStatus.Error(Exception()),
            existingOnly = true,
            allowUserEmailEdits = true
        )
    }

    @Test
    fun `onCreate returns Failed when Authentication with email edits disabled and SignedOut`() = runTest {
        testAuthenticationFailureCase(
            accountStatus = AccountStatus.SignedOut,
            existingOnly = false,
            allowUserEmailEdits = false
        )
    }

    @Test
    fun `onCreate returns Failed when Authentication with email edits disabled and Error`() = runTest {
        testAuthenticationFailureCase(
            accountStatus = AccountStatus.Error(Exception()),
            existingOnly = false,
            allowUserEmailEdits = false
        )
    }

    @Test
    fun `onCreate navigates to SignUp when Authentication with email edits enabled and SignedOut`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
            allowUserEmailEdits = true
        )

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkLaunchMode = LinkLaunchMode.Authentication(existingOnly = false),
            linkConfiguration = linkConfiguration
        )
        linkAccountManager.setAccountStatus(AccountStatus.SignedOut)

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.SignUp)
    }

    @Test
    fun `onCreate shows VerificationDialog when Authentication existingOnly with Verified status but no SMS session`() =
        runTest {
            val linkAccountManager = FakeLinkAccountManager()
            linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

            val vm = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authentication(existingOnly = true)
            )
            linkAccountManager.setAccountStatus(
                AccountStatus.Verified(
                    hasVerifiedSMSSession = false,
                    consentPresentation = null
                )
            )

            vm.onCreate(mock())

            advanceUntilIdle()

            assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))
        }

    private fun testAuthenticationFailureCase(
        accountStatus: AccountStatus,
        existingOnly: Boolean,
        allowUserEmailEdits: Boolean
    ) = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
            allowUserEmailEdits = allowUserEmailEdits
        )

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkLaunchMode = LinkLaunchMode.Authentication(existingOnly = existingOnly),
            linkConfiguration = linkConfiguration
        )
        linkAccountManager.setAccountStatus(accountStatus)

        vm.result.test {
            vm.onCreate(mock())

            advanceUntilIdle()

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkActivityResult.Failed::class.java)
            val failedResult = result as LinkActivityResult.Failed
            assertThat(failedResult.linkAccountUpdate).isEqualTo(LinkAccountUpdate.None)
        }
    }

    @Test
    fun `onCreate should navigate to signUp screen when account status is Error`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.Error(Exception()))

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.SignUp)
    }

    @Test
    fun `onCreate should launch web when attestation check fails`() = runTest {
        var launchWebConfig: LinkConfiguration? = null
        val error = Throwable("oops")
        val linkAttestationCheck = FakeLinkAttestationCheck()
        linkAttestationCheck.result = LinkAttestationCheck.Result.AttestationFailed(error)

        val vm = createViewModel(
            linkAttestationCheck = linkAttestationCheck,
            launchWeb = { config, _ ->
                launchWebConfig = config
            }
        )

        vm.onCreate(mock())

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.Loading)

        assertThat(launchWebConfig).isNotNull()
    }

    @Test
    fun `onCreate shouldn't launch web when attestationCheck fails`() =
        runTest {
            var launchWebConfig: LinkConfiguration? = null
            val linkAttestationCheck = FakeLinkAttestationCheck()

            val vm = createViewModel(
                linkAttestationCheck = linkAttestationCheck,
                launchWeb = { config, _ ->
                    launchWebConfig = config
                }
            )

            vm.onCreate(mock())

            advanceUntilIdle()

            val state = vm.linkScreenState.value as ScreenState.FullScreen
            assertEquals(state.initialDestination, LinkScreen.SignUp)
            assertThat(launchWebConfig).isNull()
        }

    @Test
    fun `onCreate should open signup when attestation check fails on account error`() = runTest {
        val error = Throwable("oops")
        testAttestationCheckError(
            attestationCheckResult = LinkAttestationCheck.Result.AccountError(error),
        )
    }

    @Test
    fun `onCreate should open signup when attestation check fails on generic error`() = runTest {
        val error = Throwable("oops")
        testAttestationCheckError(
            attestationCheckResult = LinkAttestationCheck.Result.Error(error),
        )
    }

    @Test
    fun `onCreate should launch 2fa when eager launch is enabled`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val linkAttestationCheck = FakeLinkAttestationCheck()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkAttestationCheck = linkAttestationCheck,
            linkExpressMode = LinkExpressMode.ENABLED
        )
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

        vm.onCreate(mock())

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))
        linkAttestationCheck.awaitInvokeCall()
        linkAttestationCheck.ensureAllEventsConsumed()
    }

    @Test
    fun `onCreate should dismiss 2fa on when succeeded`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkExpressMode = LinkExpressMode.ENABLED
        )
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

        vm.linkScreenState.test {
            assertThat(awaitItem()).isEqualTo(ScreenState.Loading)
            vm.onCreate(mock())
            assertThat(awaitItem()).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

            linkAccountManager.setAccountStatus(AccountStatus.Verified(true, null))
            vm.onVerificationSucceeded(null)
            assertThat(awaitItem()).isInstanceOf(ScreenState.FullScreen::class.java)
        }
    }

    @Test
    fun `onCreate should dismiss 2fa on when dismissed`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkExpressMode = LinkExpressMode.ENABLED
        )

        vm.result.test {
            linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification())

            vm.onCreate(mock())

            assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

            vm.onDismissVerificationClicked()

            assertThat(awaitItem()).isEqualTo(
                LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
            )
        }
    }

    @Test
    fun `sign up route has correct app bar config`() {
        val viewModel = createViewModel()

        viewModel.onNavEntryChanged(
            NavBackStackEntryUpdate(
                previousBackStackEntry = null,
                currentBackStackEntry = navBackStackEntry(LinkScreen.SignUp)
            )
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.canNavigateBack).isFalse()
    }

    private fun navBackStackEntry(screen: LinkScreen): NavBackStackEntry {
        val mockDestination = mock<NavDestination> {
            on { route } doReturn screen.route
        }

        return mock<NavBackStackEntry> {
            on { destination } doReturn mockDestination
        }
    }

    @Test
    fun `verification route has correct app bar config`() {
        val viewModel = createViewModel()

        viewModel.onNavEntryChanged(
            NavBackStackEntryUpdate(
                previousBackStackEntry = null,
                currentBackStackEntry = navBackStackEntry(LinkScreen.Verification)
            )
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.canNavigateBack).isFalse()
    }

    @Test
    fun `wallet route has correct app bar config`() {
        val viewModel = createViewModel()

        viewModel.onNavEntryChanged(
            NavBackStackEntryUpdate(
                previousBackStackEntry = null,
                currentBackStackEntry = navBackStackEntry(LinkScreen.Wallet)
            )
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.canNavigateBack).isFalse()
    }

    @Test
    fun `payment method route has correct app bar config`() {
        val viewModel = createViewModel()

        viewModel.onNavEntryChanged(
            NavBackStackEntryUpdate(
                previousBackStackEntry = null,
                currentBackStackEntry = navBackStackEntry(LinkScreen.PaymentMethod)
            )
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.showHeader).isFalse()
        assertThat(appBarState.canNavigateBack).isFalse()
    }

    @Test
    fun `loading route has correct app bar config`() {
        val viewModel = createViewModel()

        viewModel.onNavEntryChanged(
            NavBackStackEntryUpdate(
                previousBackStackEntry = null,
                currentBackStackEntry = navBackStackEntry(LinkScreen.Loading)
            )
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.canNavigateBack).isFalse()
    }

    @Test
    fun `logout action should dismiss screen`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
        )

        viewModel.result.test {
            viewModel.handleViewAction(LinkAction.LogoutClicked)

            linkAccountManager.awaitLogoutCall()
            assertThat(awaitItem()).isEqualTo(
                LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                    linkAccountUpdate = LinkAccountUpdate.Value(null, LoggedOut)
                )
            )
        }
    }

    @Test
    fun `change email should navigate to email route and update savedStateHandle`() {
        val navigationManager = TestNavigationManager()
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(
            navigationManager = navigationManager,
            savedStateHandle = savedStateHandle
        )

        viewModel.changeEmail()

        assertThat(savedStateHandle.get<Boolean>(SignUpViewModel.USE_LINK_CONFIGURATION_CUSTOMER_INFO)).isFalse()

        navigationManager.assertNavigatedTo(
            route = LinkScreen.SignUp.route,
            popUpTo = PopUpToBehavior.Start
        )
    }

    @Test
    fun `blocks dismissal if LinkDismissalCoordinator disables it`() = runTest {
        val dismissalCoordinator = RealLinkDismissalCoordinator()
        val viewModel = createViewModel(
            activityRetainedComponent = FakeNativeLinkComponent(
                dismissalCoordinator = dismissalCoordinator,
            ),
        )

        viewModel.result.test {
            dismissalCoordinator.setDismissible(false)
            viewModel.dismissSheet()
            expectNoEvents()

            dismissalCoordinator.setDismissible(true)
            viewModel.dismissSheet()
            assertThat(awaitItem()).isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                )
            )
        }
    }

    @Test
    fun `moveToWeb with Authorization launch mode should fail`() = runTest {
        val vm = createViewModel(
            linkLaunchMode = LinkLaunchMode.Authorization(linkAuthIntentId = "lai_123")
        )

        vm.result.test {
            vm.moveToWeb(RuntimeException("test error"))

            val result = awaitItem()
            assertThat(result).isInstanceOf(LinkActivityResult.Failed::class.java)
        }
    }

    @Test
    fun `onCreate with Authorization mode and Verified status with Inline consent should complete immediately`() =
        runTest {
            val linkAccountManager = FakeLinkAccountManager()
            val inlineConsentPresentation = mock<ConsentPresentation.Inline>()

            val linkAuthIntentInfo = LinkAuthIntentInfo(
                linkAuthIntentId = "lai_123",
                consentPresentation = inlineConsentPresentation
            )
            val linkAccount = TestFactory.LINK_ACCOUNT.copy(
                linkAuthIntentInfo = linkAuthIntentInfo
            )
            linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(linkAccount))

            val vm = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authorization(linkAuthIntentId = "lai_123")
            )
            linkAccountManager.setAccountStatus(AccountStatus.Verified(true, inlineConsentPresentation))

            vm.result.test {
                vm.onCreate(mock())
                advanceUntilIdle()

                val result = awaitItem() as LinkActivityResult.Completed
                assertThat(result.linkAccountUpdate).isEqualTo(linkAccountManager.linkAccountUpdate)
            }
        }

    @Test
    fun `buildFullScreenState with Authorization mode should navigate to OAuthConsent when Verified`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val linkAccount = TestFactory.LINK_ACCOUNT
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(linkAccount))

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkLaunchMode = LinkLaunchMode.Authorization(linkAuthIntentId = "lai_123")
        )
        linkAccountManager.setAccountStatus(
            AccountStatus.Verified(
                hasVerifiedSMSSession = true,
                consentPresentation = ConsentPresentation.FullScreen(TestFactory.CONSENT_PANE)
            )
        )

        vm.onCreate(mock())
        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.OAuthConsent)
    }

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh should complete with consent granted when status is Consented`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Consented,
            assertionBlock = {
                val activityResult = awaitItem() as LinkActivityResult.Completed
                assertThat(activityResult.authorizationConsentGranted).isTrue()
            }
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh should complete with consent rejected when status is Rejected`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Rejected,
            assertionBlock = {
                val activityResult = awaitItem() as LinkActivityResult.Completed
                assertThat(activityResult.authorizationConsentGranted).isFalse()
            }
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh should fail when status is Created`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Created,
            assertionBlock = {
                val activityResult = awaitItem() as LinkActivityResult.Failed
                assertThat(activityResult.error.message)
                    .contains("Unexpected LAI status when account is verified: Created")
            }
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh should fail when status is Expired`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Expired,
            assertionBlock = {
                val activityResult = awaitItem() as LinkActivityResult.Failed
                assertThat(activityResult.error.message)
                    .contains("Unexpected LAI status when account is verified: Expired")
            }
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh navigates to OAuthConsent with Authenticated FullScreen consent`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Authenticated,
            consentPresentation = ConsentPresentation.FullScreen(TestFactory.CONSENT_PANE),
            expectedScreenState = ScreenState.FullScreen(LinkScreen.OAuthConsent)
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh completes with Authenticated non-FullScreen consent`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = LinkAuthIntent.Status.Authenticated,
            consentPresentation = mock<ConsentPresentation.Inline>(),
            assertionBlock = {
                val activityResult = awaitItem() as LinkActivityResult.Completed
                assertThat(activityResult.linkAccountUpdate).isNotNull()
            }
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh navigates to OAuthConsent with null LAI FullScreen consent`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = null,
            consentPresentation = ConsentPresentation.FullScreen(TestFactory.CONSENT_PANE),
            expectedScreenState = ScreenState.FullScreen(LinkScreen.OAuthConsent)
        )

    @Test
    fun `getScreenStateForAuthorizationAfterRefresh completes with null linkAuthIntent non-FullScreen consent`() =
        testGetScreenStateForAuthorizationAfterRefresh(
            linkAuthIntentStatus = null,
            consentPresentation = mock<ConsentPresentation.Inline>(),
            assertionBlock = {
                assertThat(awaitItem()).isInstanceOf(LinkActivityResult.Completed::class.java)
            }
        )

    private fun testGetScreenStateForAuthorizationAfterRefresh(
        linkAuthIntentStatus: LinkAuthIntent.Status?,
        consentPresentation: ConsentPresentation? = null,
        expectedScreenState: ScreenState? = null,
        assertionBlock: (suspend TurbineTestContext<LinkActivityResult>.() -> Unit)? = null
    ) = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(linkAccountManager = linkAccountManager)

        val accountStatus = AccountStatus.Verified(true, consentPresentation)
        val consumerSessionRefresh = ConsumerSessionRefresh(
            consumerSession = TestFactory.CONSUMER_SESSION,
            linkAuthIntent = linkAuthIntentStatus?.let { LinkAuthIntent(it) }
        )

        if (expectedScreenState != null) {
            // Test case expecting a specific ScreenState return value
            val result = vm.getScreenStateForAuthorizationAfterRefresh(accountStatus, consumerSessionRefresh)
            assertThat(result).isEqualTo(expectedScreenState)
        }
        if (assertionBlock != null) {
            // Test case expecting null return with result emissions
            vm.result.test {
                val result = vm.getScreenStateForAuthorizationAfterRefresh(accountStatus, consumerSessionRefresh)
                assertThat(result).isNull()
                assertionBlock()
            }
        }
    }

    private fun testAttestationCheckError(
        attestationCheckResult: LinkAttestationCheck.Result,
    ) = runTest {
        var launchWebConfig: LinkConfiguration? = null
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val linkAccountManager = FakeLinkAccountManager(
            linkAccountHolder = linkAccountHolder,
            accountStatusOverride = linkAccountHolder.linkAccountInfo.map {
                it.account?.accountStatus ?: AccountStatus.SignedOut
            }
        )
        val linkAttestationCheck = FakeLinkAttestationCheck()

        linkAttestationCheck.result = attestationCheckResult
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val vm = createViewModel(
            linkAttestationCheck = linkAttestationCheck,
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            linkExpressMode = LinkExpressMode.DISABLED,
            launchWeb = { config, _ ->
                launchWebConfig = config
            },
        )

        vm.result.test {
            vm.onCreate(mock())

            advanceUntilIdle()

            linkAccountManager.awaitLogoutCall()
            assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
            assertThat(launchWebConfig).isNull()

            expectNoEvents()

            val state = vm.linkScreenState.value as ScreenState.FullScreen
            assertEquals(state.initialDestination, LinkScreen.SignUp)
        }
    }

    private fun createViewModel(
        activityRetainedComponent: NativeLinkComponent = FakeNativeLinkComponent(),
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        eventReporter: EventReporter = FakeEventReporter(),
        navigationManager: NavigationManager = TestNavigationManager(),
        linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
        linkExpressMode: LinkExpressMode = LinkExpressMode.DISABLED,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full,
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        launchWeb: (LinkConfiguration, PaymentMethodMetadata) -> Unit = { _, _ -> },
        autocompleteLauncher: AutocompleteActivityLauncher = TestAutocompleteLauncher.noOp(),
        linkConfiguration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        addPaymentMethodOptionsFactory: AddPaymentMethodOptions.Factory = mock(),
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            activityRetainedComponent = activityRetainedComponent,
            eventReporter = eventReporter,
            confirmationHandlerFactory = { confirmationHandler },
            linkAttestationCheck = linkAttestationCheck,
            linkConfiguration = linkConfiguration,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkExpressMode = linkExpressMode,
            navigationManager = navigationManager,
            savedStateHandle = savedStateHandle,
            linkLaunchMode = linkLaunchMode,
            linkConfirmationHandlerFactory = { linkConfirmationHandler },
            autocompleteLauncher = autocompleteLauncher,
            addPaymentMethodOptionsFactory = addPaymentMethodOptionsFactory,
        ).apply {
            this.launchWebFlow = launchWeb
        }
    }

    private fun creationExtras(): CreationExtras {
        val mockOwner = mock<SavedStateRegistryOwner>()
        val mockViewModelStoreOwner = mock<ViewModelStoreOwner>()
        whenever(mockViewModelStoreOwner.viewModelStore).thenReturn(ViewModelStore())
        return MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, mockOwner)
            set(APPLICATION_KEY, application)
            set(VIEW_MODEL_STORE_OWNER_KEY, mockViewModelStoreOwner)
            set(VIEW_MODEL_KEY, LinkActivityViewModel::class.java.name)
        }
    }
}
