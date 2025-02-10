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
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.PopUpToBuilder
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@SuppressWarnings("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `test that cancel result is called on back pressed with empty stack`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        val navController = navController()
        whenever(navController.popBackStack()).thenReturn(false)

        var result: LinkActivityResult? = null
        fun dismissWithResult(actualResult: LinkActivityResult) {
            result = actualResult
        }

        val vm = createViewModel(
            navController = navController,
            linkAccountManager = linkAccountManager,
            dismissWithResult = ::dismissWithResult
        )

        vm.handleViewAction(LinkAction.BackPressed)

        verify(navController).popBackStack()
        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
                )
            )
    }

    @Test
    fun `test that cancel result is called on back pressed with non-empty stack`() = runTest(dispatcher) {
        val navController = navController()
        whenever(navController.popBackStack()).thenReturn(true)

        var result: LinkActivityResult? = null
        fun dismissWithResult(actualResult: LinkActivityResult) {
            result = actualResult
        }

        val vm = createViewModel(navController = navController, dismissWithResult = ::dismissWithResult)

        vm.handleViewAction(LinkAction.BackPressed)

        verify(navController).popBackStack()
        assertThat(result).isNull()
    }

    @Test
    fun `test that activity unregister removes dismissWithResult and nav controller`() = runTest(dispatcher) {
        val vm = createViewModel()

        assertThat(vm.dismissWithResult).isNotNull()
        assertThat(vm.navController).isNotNull()

        vm.unregisterActivity()

        assertThat(vm.dismissWithResult).isEqualTo(null)
        assertThat(vm.navController).isEqualTo(null)
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
            linkAccount = null
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
    fun `navigate should call NavController navigate with correct route, should clear stack when clearStack is true`() {
        val navController = navController()

        val vm = createViewModel()
        vm.navController = navController

        vm.navigate(LinkScreen.SignUp, clearStack = true)

        assertNavigation(navController = navController, screen = LinkScreen.SignUp, clearStack = true)
    }

    @Test
    fun `navigate should call NavController navigate with correct route, should not clear when clearStack is false`() {
        val navController = navController()

        val vm = createViewModel()
        vm.navController = navController

        vm.navigate(LinkScreen.Wallet, clearStack = false)

        assertNavigation(navController = navController, screen = LinkScreen.Wallet, clearStack = false)
    }

    @Test
    fun `onCreate should navigate to wallet screen when account status is Verified`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Wallet,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `onCreate should navigate to verification screen when account status is NeedsVerification`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Verification,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `onCreate should navigate to verification screen when account status is VerificationStarted`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.VerificationStarted)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Verification,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `onCreate should navigate to signUp screen when account status is SignedOut`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.SignedOut)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.SignUp,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `onCreate should navigate to signUp screen when account status is Error`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Error)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.SignUp,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `onCreate should launch web when attestation check fails`() = runTest {
        var launchWebConfig: LinkConfiguration? = null
        val navController = navController()
        val error = Throwable("oops")
        val linkAttestationCheck = FakeLinkAttestationCheck()
        linkAttestationCheck.result = LinkAttestationCheck.Result.AttestationFailed(error)

        val vm = createViewModel(
            linkAttestationCheck = linkAttestationCheck,
            launchWeb = { config ->
                launchWebConfig = config
            }
        )
        vm.navController = navController

        vm.onCreate(mock())

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Loading,
            clearStack = true,
            launchSingleTop = false
        )
        assertThat(launchWebConfig).isNotNull()
    }

    @Test
    fun `onCreate shouldn't launch web when attestationCheck fails`() =
        runTest {
            var launchWebConfig: LinkConfiguration? = null
            val navController = navController()
            val linkAttestationCheck = FakeLinkAttestationCheck()

            val vm = createViewModel(
                linkAttestationCheck = linkAttestationCheck,
                launchWeb = { config ->
                    launchWebConfig = config
                }
            )
            vm.navController = navController

            vm.onCreate(mock())

            advanceUntilIdle()

            assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.FullScreen)
            assertThat(launchWebConfig).isNull()
        }

    @Test
    fun `onCreate should launch 2fa when eager launch is enabled`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        val navController = navController()

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            startWithVerificationDialog = true
        )
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        advanceUntilIdle()

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))
    }

    @Test
    fun `onCreate should dismiss 2fa on when succeeded`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        val navController = navController()

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            startWithVerificationDialog = true
        )
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        advanceUntilIdle()

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

        vm.onVerificationSucceeded()

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.FullScreen)
    }

    @Test
    fun `onCreate should dismiss 2fa on when dismissed`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        val navController = navController()

        var activityResult: LinkActivityResult? = null
        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            startWithVerificationDialog = true,
            dismissWithResult = {
                activityResult = it
            }
        )
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.NeedsVerification)

        vm.onCreate(mock())

        advanceUntilIdle()

        assertThat(vm.linkScreenState.value).isEqualTo(ScreenState.VerificationDialog(TestFactory.LINK_ACCOUNT))

        vm.onDismissVerificationClicked()

        assertThat(activityResult)
            .isEqualTo(
                LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
            )
    }

    @Test
    fun `sign up route has correct app bar config`() {
        val navController = navController(LinkScreen.SignUp)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
    }

    @Test
    fun `verification route has correct app bar config`() {
        val navController = navController(LinkScreen.Verification)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
    }

    @Test
    fun `wallet route has correct app bar config`() {
        val navController = navController(LinkScreen.Wallet)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isTrue()
        assertThat(appBarState.showOverflowMenu).isTrue()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_close)
    }

    @Test
    fun `payment method route has correct app bar config`() {
        val navController = navController(LinkScreen.PaymentMethod)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isFalse()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_back)
    }

    @Test
    fun `card edit route has correct app bar config`() {
        val navController = navController(LinkScreen.CardEdit)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isFalse()
        assertThat(appBarState.showOverflowMenu).isFalse()
        assertThat(appBarState.navigationIcon).isEqualTo(R.drawable.stripe_link_back)
    }

    @Test
    fun `loading route has correct app bar config`() {
        val navController = navController(LinkScreen.Loading)
        val viewModel = createViewModel(
            navController = navController
        )

        val appBarState = viewModel.linkAppBarState.value
        assertThat(appBarState.email).isNull()
        assertThat(appBarState.showHeader).isFalse()
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
    fun `linkScreenScreenCreated should not navigate when screen is not loading`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController(screen = LinkScreen.SignUp)

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        verify(navController, times(0)).navigate(
            any(),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `linkScreenScreenCreated should navigate when screen is loading`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Wallet,
            clearStack = true,
            launchSingleTop = true
        )
    }

    @Test
    fun `linkScreenScreenCreated should navigate when screen is null`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController(screen = null)

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())
        vm.linkScreenCreated()

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.Wallet,
            clearStack = true,
            launchSingleTop = true
        )
    }

    private fun navController(
        screen: LinkScreen? = LinkScreen.Loading
    ): NavHostController {
        val currentBackStackEntry = screen?.let {
            NavBackStackEntry.create(
                context = null,
                destination = NavDestination("").apply {
                    route = screen.route
                }
            )
        }
        val navController: NavHostController = mock()
        val mockGraph: NavGraph = mock()
        `when`(mockGraph.id).thenReturn(FAKE_GRAPH_ID)
        `when`(navController.graph).thenReturn(mockGraph)
        `when`(navController.currentBackStackEntry).thenReturn(currentBackStackEntry)
        `when`(navController.currentBackStackEntryFlow).thenReturn(
            currentBackStackEntry?.let {
                flowOf(it)
            } ?: flowOf()
        )
        return navController
    }

    private fun assertNavigation(
        navController: NavHostController,
        screen: LinkScreen,
        clearStack: Boolean,
        launchSingleTop: Boolean = false
    ) {
        verify(navController).navigate(
            eq(screen.route),
            any<NavOptionsBuilder.() -> Unit>()
        )

        val navOptionsLambdaCaptor = argumentCaptor<NavOptionsBuilder.() -> Unit>()
        verify(navController).navigate(eq(screen.route), navOptionsLambdaCaptor.capture())

        val capturedLambda = navOptionsLambdaCaptor.firstValue
        val navOptionsBuilder = spy(NavOptionsBuilder())
        capturedLambda.invoke(navOptionsBuilder)

        assertThat(navOptionsBuilder.launchSingleTop).isEqualTo(launchSingleTop)

        if (clearStack) {
            val popUpToLambdaCaptor = argumentCaptor<PopUpToBuilder.() -> Unit>()
            verify(navOptionsBuilder).popUpTo(eq(FAKE_GRAPH_ID), popUpToLambdaCaptor.capture())

            val capturedPopUpToLambda = popUpToLambdaCaptor.firstValue
            val popUpToBuilder = PopUpToBuilder()
            capturedPopUpToLambda.invoke(popUpToBuilder)

            assertThat(popUpToBuilder.inclusive).isTrue()
        } else {
            verify(navOptionsBuilder, times(0)).popUpTo(any<String>(), any<PopUpToBuilder.() -> Unit>())
        }
    }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        eventReporter: EventReporter = FakeEventReporter(),
        navController: NavHostController = navController(),
        linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
        startWithVerificationDialog: Boolean = false,
        dismissWithResult: (LinkActivityResult) -> Unit = {},
        launchWeb: (LinkConfiguration) -> Unit = {}
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            linkAccountManager = linkAccountManager,
            activityRetainedComponent = FakeNativeLinkComponent(),
            eventReporter = eventReporter,
            confirmationHandlerFactory = { confirmationHandler },
            linkAttestationCheck = linkAttestationCheck,
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            startWithVerificationDialog = startWithVerificationDialog,
        ).apply {
            this.navController = navController
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

    companion object {
        private const val FAKE_GRAPH_ID = 123
    }
}
