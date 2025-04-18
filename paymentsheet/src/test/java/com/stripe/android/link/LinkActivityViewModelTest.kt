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
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.navigation.NavBackStackEntryUpdate
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import com.stripe.android.utils.DummyActivityResultCaller
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
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        var result: LinkActivityResult? = null
        fun dismissWithResult(actualResult: LinkActivityResult) {
            result = actualResult
        }

        val vm = createViewModel(
            navigationManager = navigationManager,
            linkAccountManager = linkAccountManager,
            dismissWithResult = ::dismissWithResult
        )

        vm.handleViewAction(LinkAction.BackPressed)

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
                )
            )
    }

    @Test
    fun `test that activity unregister removes dismissWithResult`() = runTest(dispatcher) {
        val vm = createViewModel()

        assertThat(vm.dismissWithResult).isNotNull()

        vm.unregisterActivity()

        assertThat(vm.dismissWithResult).isNull()
    }

    @Test
    fun `test that activity registerActivityForConfirmation registers confirmation handler`() = runTest(dispatcher) {
        val confirmationHandler = FakeConfirmationHandler()
        val vm = createViewModel(
            confirmationHandler = confirmationHandler
        )

        vm.registerActivityForConfirmation(
            activityResultCaller = DummyActivityResultCaller.noOp(),
            lifecycleOwner = object : LifecycleOwner {
                override val lifecycle: Lifecycle = mock()
            }
        )

        vm.unregisterActivity()

        val registerCall = confirmationHandler.registerTurbine.awaitItem()

        assertThat(registerCall).isNotNull()
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
            configuration = mock(),
            publishableKey = "",
            stripeAccountId = null,
            startWithVerificationDialog = false,
            linkAccount = null,
            paymentElementCallbackIdentifier = "LinkNativeTestIdentifier",
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

        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

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
    fun `onCreate should start with Wallet screen account status is Verified`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())

        advanceUntilIdle()

        val state = vm.linkScreenState.value as ScreenState.FullScreen
        assertEquals(state.initialDestination, LinkScreen.Wallet)
    }

    @Test
    fun `onCreate should start with verification screen when account status is NeedsVerification`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

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
    fun `onCreate should navigate to signUp screen when account status is Error`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setAccountStatus(AccountStatus.Error)

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
            launchWeb = { config ->
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
                launchWeb = { config ->
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
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkAttestationCheck = linkAttestationCheck,
            startWithVerificationDialog = true
        )
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))
        linkAttestationCheck.ensureAllEventsConsumed()
    }

    @Test
    fun `onCreate should dismiss 2fa on when succeeded`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            startWithVerificationDialog = true
        )
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        advanceUntilIdle()

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

        vm.onVerificationSucceeded()

        advanceUntilIdle()

        assertThat(vm.linkScreenState.value).isInstanceOf(ScreenState.FullScreen::class.java)
    }

    @Test
    fun `onCreate should dismiss 2fa on when dismissed`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        var activityResult: LinkActivityResult? = null
        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            startWithVerificationDialog = true,
            dismissWithResult = {
                activityResult = it
            }
        )
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

        vm.onDismissVerificationClicked()

        assertThat(activityResult)
            .isEqualTo(
                LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
            )
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
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
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
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
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
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isTrue()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
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
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isFalse()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
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
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
    }

    @Test
    fun `logout action should dismiss screen`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            dismissWithResult = {
                result = it
            }
        )

        viewModel.handleViewAction(LinkAction.LogoutClicked)

        linkAccountManager.awaitLogoutCall()
        assertThat(result).isEqualTo(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )
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

    private fun testAttestationCheckError(
        attestationCheckResult: LinkAttestationCheck.Result,
        expectedLinkActivityResult: LinkActivityResult? = null,
    ) = runTest {
        var launchWebConfig: LinkConfiguration? = null
        var result: LinkActivityResult? = null
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val linkAccountManager = FakeLinkAccountManager(
            linkAccountHolder = linkAccountHolder,
            accountStatusOverride = linkAccountHolder.linkAccount.map {
                it?.accountStatus ?: AccountStatus.SignedOut
            }
        )
        val linkAttestationCheck = FakeLinkAttestationCheck()

        linkAttestationCheck.result = attestationCheckResult
        linkAccountHolder.set(TestFactory.LINK_ACCOUNT)
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        val vm = createViewModel(
            linkAttestationCheck = linkAttestationCheck,
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            startWithVerificationDialog = false,
            launchWeb = { config ->
                launchWebConfig = config
            },
            dismissWithResult = {
                result = it
            }
        )
        vm.onCreate(mock())

        advanceUntilIdle()

        linkAccountManager.awaitLogoutCall()
        assertThat(linkAccountHolder.linkAccount.value).isNull()
        assertThat(launchWebConfig).isNull()
        assertThat(result).isEqualTo(expectedLinkActivityResult)

        val state = vm.linkScreenState.value as ScreenState.FullScreen

        assertEquals(state.initialDestination, LinkScreen.SignUp)
    }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        eventReporter: EventReporter = FakeEventReporter(),
        navigationManager: NavigationManager = TestNavigationManager(),
        linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
        startWithVerificationDialog: Boolean = false,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        dismissWithResult: (LinkActivityResult) -> Unit = {},
        launchWeb: (LinkConfiguration) -> Unit = {}
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            activityRetainedComponent = FakeNativeLinkComponent(),
            eventReporter = eventReporter,
            confirmationHandlerFactory = { confirmationHandler },
            linkAttestationCheck = linkAttestationCheck,
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            startWithVerificationDialog = startWithVerificationDialog,
            navigationManager = navigationManager,
            savedStateHandle = savedStateHandle
        ).apply {
            this.dismissWithResult = dismissWithResult
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
