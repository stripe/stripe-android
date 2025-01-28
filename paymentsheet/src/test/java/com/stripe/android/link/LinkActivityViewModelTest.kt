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
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.PopUpToBuilder
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.attestation.IntegrityRequestManager
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

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

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

    @Test
    fun `onCreate should launch web when attestation fails and useAttestationEndpoints is enabled`() = runTest {
        var launchWebConfig: LinkConfiguration? = null
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()
        val linkGate = FakeLinkGate()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val errorReporter = FakeErrorReporter()

        integrityRequestManager.prepareResult = Result.failure(Throwable("oops"))
        linkGate.setUseAttestationEndpoints(true)

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkGate = linkGate,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter,
            launchWeb = { config ->
                launchWebConfig = config
            }
        )
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())

        advanceUntilIdle()

        integrityRequestManager.awaitPrepareCall()
        assertNavigation(
            navController = navController,
            screen = LinkScreen.Loading,
            clearStack = true,
            launchSingleTop = false
        )
        assertThat(launchWebConfig).isNotNull()
        assertThat(errorReporter.getLoggedErrors())
            .containsExactly(
                ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER.eventName
            )
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `onCreate shouldn't launch web when integrity preparation passes and useAttestationEndpoints is enabled`() =
        runTest {
            var launchWebConfig: LinkConfiguration? = null
            val linkAccountManager = FakeLinkAccountManager()
            val navController = navController()
            val linkGate = FakeLinkGate()
            val integrityRequestManager = FakeIntegrityRequestManager()

            linkGate.setUseAttestationEndpoints(true)

            val vm = createViewModel(
                linkAccountManager = linkAccountManager,
                linkGate = linkGate,
                integrityRequestManager = integrityRequestManager,
                launchWeb = { config ->
                    launchWebConfig = config
                }
            )
            vm.navController = navController
            linkAccountManager.setAccountStatus(AccountStatus.Verified)

            vm.onCreate(mock())

            advanceUntilIdle()

            integrityRequestManager.awaitPrepareCall()
            assertNavigation(
                navController = navController,
                screen = LinkScreen.Wallet,
                clearStack = true,
                launchSingleTop = true
            )
            assertThat(launchWebConfig).isNull()
            integrityRequestManager.ensureAllEventsConsumed()
        }

    @Test
    fun `onCreate should not prepare integrity when useAttestationEndpoints is disabled`() = runTest {
        var launchWebConfig: LinkConfiguration? = null
        val linkAccountManager = FakeLinkAccountManager()
        val navController = navController()
        val linkGate = FakeLinkGate()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkGate.setUseAttestationEndpoints(false)

        val vm = createViewModel(
            linkAccountManager = linkAccountManager,
            linkGate = linkGate,
            integrityRequestManager = integrityRequestManager,
            launchWeb = { config ->
                launchWebConfig = config
            }
        )
        vm.navController = navController
        linkAccountManager.setAccountStatus(AccountStatus.Verified)

        vm.onCreate(mock())

        advanceUntilIdle()

        assertThat(launchWebConfig).isNull()
        integrityRequestManager.ensureAllEventsConsumed()
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
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        confirmationHandler: ConfirmationHandler = FakeConfirmationHandler(),
        eventReporter: EventReporter = FakeEventReporter(),
        navController: NavHostController = navController(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager(),
        linkGate: LinkGate = FakeLinkGate(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        dismissWithResult: (LinkActivityResult) -> Unit = {},
        launchWeb: (LinkConfiguration) -> Unit = {}
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            linkAccountManager = linkAccountManager,
            activityRetainedComponent = FakeNativeLinkComponent(),
            eventReporter = eventReporter,
            confirmationHandlerFactory = { confirmationHandler },
            integrityRequestManager = integrityRequestManager,
            linkGate = linkGate,
            errorReporter = errorReporter
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

private class FakeNativeLinkComponent(
    override val linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
    override val configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
    override val linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
    override val logger: Logger = FakeLogger(),
    override val linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory = LinkConfirmationHandler.Factory {
        FakeLinkConfirmationHandler()
    },
    override val webLinkActivityContract: WebLinkActivityContract = mock(),
    override val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory =
        NullCardAccountRangeRepositoryFactory,
    override val viewModel: LinkActivityViewModel = mock()
) : NativeLinkComponent
