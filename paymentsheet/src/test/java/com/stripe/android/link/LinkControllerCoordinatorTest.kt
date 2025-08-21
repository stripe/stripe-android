package com.stripe.android.link

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakeActivityResultRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class LinkControllerCoordinatorTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    private val linkActivityContract: NativeLinkActivityContract = mock()

    private val presentPaymentMethodsResultFlow = MutableSharedFlow<LinkController.PresentPaymentMethodsResult>()
    private val authenticationResultFlow = MutableSharedFlow<LinkController.AuthenticationResult>()
    private val authorizeResultFlow = MutableSharedFlow<LinkController.AuthorizeResult>()

    private val viewModel: LinkControllerInteractor = mock {
        on { presentPaymentMethodsResultFlow } doReturn presentPaymentMethodsResultFlow
        on { authenticationResultFlow } doReturn authenticationResultFlow
        on { authorizeResultFlow } doReturn authorizeResultFlow
    }

    private val presentPaymentMethodsResults = mutableListOf<LinkController.PresentPaymentMethodsResult>()
    private val authenticationResults = mutableListOf<LinkController.AuthenticationResult>()
    private val authorizeResults = mutableListOf<LinkController.AuthorizeResult>()

    private val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)

    private fun createCoordinator(
        activityResult: LinkActivityResult? = null
    ): LinkControllerCoordinator {
        val registry = FakeActivityResultRegistry(activityResult)
        val activityResultRegistryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }

        return LinkControllerCoordinator(
            interactor = viewModel,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            linkActivityContract = linkActivityContract,
            selectedPaymentMethodCallback = { presentPaymentMethodsResults.add(it) },
            authenticationCallback = { authenticationResults.add(it) },
            authorizeCallback = { authorizeResults.add(it) },
        )
    }

    @Test
    fun `constructor() throws when lifecycle is not INITIALIZED`() = runTest {
        assertFailsWith<IllegalStateException> {
            lifecycleOwner.setCurrentState(Lifecycle.State.DESTROYED)
            createCoordinator()
        }
    }

    @Test
    fun `constructor() succeeds when lifecycle is INITIALIZED`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.INITIALIZED)
        val coordinator = createCoordinator()
        assertThat(coordinator.linkActivityResultLauncher).isNotNull()
    }

    @Test
    fun `when started, flows are collected and callbacks invoked`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.STARTED)
        createCoordinator()

        val presentResult = LinkController.PresentPaymentMethodsResult.Success
        presentPaymentMethodsResultFlow.emit(presentResult)

        val authResult = LinkController.AuthenticationResult.Success
        authenticationResultFlow.emit(authResult)

        val authorizeResult = LinkController.AuthorizeResult.Denied
        authorizeResultFlow.emit(authorizeResult)

        assertThat(presentPaymentMethodsResults).containsExactly(presentResult)
        assertThat(authenticationResults).containsExactly(authResult)
        assertThat(authorizeResults).containsExactly(authorizeResult)
    }

    @Test
    fun `when not started, flows are not collected`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.CREATED)
        createCoordinator()

        presentPaymentMethodsResultFlow.emit(
            LinkController.PresentPaymentMethodsResult.Success
        )
        authenticationResultFlow.emit(LinkController.AuthenticationResult.Success)
        authorizeResultFlow.emit(LinkController.AuthorizeResult.Denied)

        assertThat(presentPaymentMethodsResults).isEmpty()
        assertThat(authenticationResults).isEmpty()
        assertThat(authorizeResults).isEmpty()
    }

    @Test
    fun `when started, multiple emissions are handled correctly`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.STARTED)
        createCoordinator()

        val result1 = LinkController.PresentPaymentMethodsResult.Success
        val result2 = LinkController.PresentPaymentMethodsResult.Canceled
        val result3 = LinkController.PresentPaymentMethodsResult.Failed(Exception("Error"))

        presentPaymentMethodsResultFlow.emit(result1)
        presentPaymentMethodsResultFlow.emit(result2)
        presentPaymentMethodsResultFlow.emit(result3)

        assertThat(presentPaymentMethodsResults).containsExactly(result1, result2, result3)
    }

    @Test
    fun `on activity result, result is passed to viewModel`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.CREATED)
        val testResult = LinkActivityResult.Completed(
            linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            selectedPayment = null,
            shippingAddress = null,
        )
        val coordinator = createCoordinator(
            activityResult = testResult
        )
        coordinator.linkActivityResultLauncher.launch(
            LinkActivityContract.Args(
                configuration = TestFactory.LINK_CONFIGURATION,
                linkExpressMode = LinkExpressMode.DISABLED,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                launchMode = LinkLaunchMode.PaymentMethodSelection(null),
            )
        )
        verify(viewModel).onLinkActivityResult(
            testResult,
        )
    }

    @Test
    fun `flow collection starts when lifecycle transitions to STARTED`() = runTest {
        lifecycleOwner.setCurrentState(Lifecycle.State.CREATED)
        createCoordinator()

        presentPaymentMethodsResultFlow.emit(
            LinkController.PresentPaymentMethodsResult.Success
        )
        assertThat(presentPaymentMethodsResults).isEmpty()

        lifecycleOwner.setCurrentState(Lifecycle.State.STARTED)
        presentPaymentMethodsResultFlow.emit(LinkController.PresentPaymentMethodsResult.Canceled)
        assertThat(presentPaymentMethodsResults).containsExactly(LinkController.PresentPaymentMethodsResult.Canceled)
    }
}
