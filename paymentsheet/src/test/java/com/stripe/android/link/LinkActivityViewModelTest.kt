package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.PopUpToBuilder
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.model.ConsumerSession
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
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

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that cancel result is called on back pressed with empty stack`() = runTest(dispatcher) {
        val navController = navController()
        whenever(navController.popBackStack()).thenReturn(false)

        var result: LinkActivityResult? = null
        fun dismissWithResult(actualResult: LinkActivityResult) {
            result = actualResult
        }

        val vm = createViewModel(navController = navController, dismissWithResult = ::dismissWithResult)

        vm.handleViewAction(LinkAction.BackPressed)

        verify(navController).popBackStack()
        assertThat(result).isEqualTo(LinkActivityResult.Canceled())
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
            stripeAccountId = null
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

    fun `vm dismisses with cancellation result on successful log out`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var callCount = 0
            override suspend fun logOut(): Result<ConsumerSession> {
                callCount += 1
                return Result.success(TestFactory.CONSUMER_SESSION)
            }
        }
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        var result: LinkActivityResult? = null
        fun dismissWithResult(r: LinkActivityResult) {
            result = r
        }

        val vm = createViewModel(linkAccountManager = linkAccountManager)
        vm.dismissWithResult = ::dismissWithResult

        vm.logout()

        assertThat(linkAccountManager.callCount).isEqualTo(1)
        assertThat(result).isEqualTo(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
    }

    @Test
    fun `vm dismisses with error log on failed log out`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)
        linkAccountManager.logOutResult = Result.failure(error)

        val logger = FakeLogger()

        var result: LinkActivityResult? = null
        fun dismissWithResult(r: LinkActivityResult) {
            result = r
        }

        val vm = createViewModel(linkAccountManager = linkAccountManager, logger = logger)
        vm.dismissWithResult = ::dismissWithResult

        vm.logout()

        assertThat(result).isEqualTo(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
        assertThat(logger.errorLogs).containsExactly("failed to log out" to error)
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

        advanceUntilIdle()

        assertNavigation(
            navController = navController,
            screen = LinkScreen.SignUp,
            clearStack = true,
            launchSingleTop = true
        )
    }

    private fun navController(): NavHostController {
        val navController: NavHostController = mock()
        val mockGraph: NavGraph = mock()
        `when`(mockGraph.id).thenReturn(FAKE_GRAPH_ID)
        `when`(navController.graph).thenReturn(mockGraph)
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
        verify(navOptionsBuilder, never()).popUpTo(any<String>(), any())
    }

    @Test
    fun `initial app bar state is correct`() = runTest {
        val vm = createViewModel()

        val initialState = vm.linkAppBarState.value
        assertThat(initialState).isEqualTo(
            LinkAppBarState(
                navigationIcon = R.drawable.stripe_link_close,
                showHeader = true,
                showOverflowMenu = false,
                email = null
            )
        )
    }

    @Test
    fun `app bar state menu and email should be visible for verified account`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT)

        advanceUntilIdle()

        assertThat(vm.linkAppBarState.value).isEqualTo(
            LinkAppBarState(
                navigationIcon = R.drawable.stripe_link_close,
                showHeader = true,
                showOverflowMenu = true,
                email = TestFactory.LINK_ACCOUNT.email
            )
        )
    }

    @Test
    fun `app bar state menu and email should be hidden for unverified account`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val vm = createViewModel(linkAccountManager = linkAccountManager)
        linkAccountManager.setLinkAccount(TestFactory.LINK_ACCOUNT_NEEDS_VERIFICATION)

        advanceUntilIdle()

        assertThat(vm.linkAppBarState.value).isEqualTo(
            LinkAppBarState(
                navigationIcon = R.drawable.stripe_link_close,
                showHeader = true,
                showOverflowMenu = false,
                email = null
            )
        )
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        logger: Logger = FakeLogger(),
        navController: NavHostController = navController(),
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            linkAccountManager = linkAccountManager,
            activityRetainedComponent = mock(),
            logger = logger
        ).apply {
            this.navController = navController
            this.dismissWithResult = dismissWithResult
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
