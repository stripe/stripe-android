@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkPaymentMethodSelectionLauncher
import com.stripe.android.link.NativeLinkActivityContract
import com.stripe.android.link.TestFactory
import com.stripe.android.link.WebLinkActivityContract
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeLinkStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutLinkPaymentOptionsPresenterTest {
    @Test
    fun `registers direct Link with unique key and host registry`() = runScenario {
        verify(linkPaymentLauncher).register(
            key = eq(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER),
            activityResultRegistry = same(activityResultRegistry),
            callback = any(),
        )
    }

    @Test
    fun `ordinary payment options delegate to default presenter`() = runScenario(
        selection = PaymentSelection.GooglePay,
    ) {
        presenter.present()

        verify(defaultPresenter).present()
        verifyLinkWasNotPresented()
    }

    @Test
    fun `eligible Link launches with exact arguments and keeps sheet open`() = runScenario {
        presenter.present()

        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        verify(linkPaymentLauncher).present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment.details),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = 123,
        )
        verify(defaultPresenter, never()).present()
    }

    @Test
    fun `completion updates selection and closes`() = runScenario {
        presenter.present()
        val updatedPayment = selectedPayment.copy(collectedCvc = "123")

        resultCallback(
            LinkActivityResult.Completed(LinkAccountUpdate.None, selectedPayment = updatedPayment)
        )

        assertThat(stateHolder.state?.paymentSelection)
            .isEqualTo(linkSelection.copy(selectedPayment = updatedPayment))
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `logout uses current customer fallback and opens payment options`() = runScenario {
        presenter.present()
        val fallback = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        customerState.value = createCustomerState(paymentMethods = listOf(fallback))

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.Saved(fallback))
        verify(defaultPresenter).present()
    }

    @Test
    fun `pay another way opens payment options`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        verify(defaultPresenter).present()
    }

    @Test
    fun `back opens payment options when Link has no usable payment`() = runScenario(
        selection = linkSelection.copy(selectedPayment = null),
    ) {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        verify(defaultPresenter).present()
    }

    @Test
    fun `back closes when Link has a usable payment`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        verify(defaultPresenter, never()).present()
    }

    @Test
    fun `failure closes and applies Link account update`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Failed(
                error = Throwable("failure"),
                linkAccountUpdate = LinkAccountUpdate.Value(null),
            )
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
        assertThat(stateHolder.state?.paymentMethodMetadata?.linkState?.loginState)
            .isEqualTo(com.stripe.android.paymentsheet.state.LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `verification dismissal persists suppression across recreation`() {
        val savedStateHandle = SavedStateHandle()
        runScenario(savedStateHandle = savedStateHandle) {
            presenter.present()
            val verifyingAccount = mock<LinkAccount> {
                on { accountStatus } doReturn AccountStatus.VerificationStarted
            }

            resultCallback(
                LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.Value(verifyingAccount),
                )
            )

            assertThat(stateHolder.state?.linkEagerPresentationSuppressed).isTrue()
        }

        runScenario(savedStateHandle = savedStateHandle) {
            sheetStateHolder.sheetIsOpen = false
            presenter.present()

            verify(defaultPresenter).present()
            verifyLinkWasNotPresented()
        }
    }

    @Test
    fun `destroy unregisters direct Link and preserves open state`() = runScenario {
        presenter.present()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(linkPaymentLauncher).unregister()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `eligible Link presentation after destruction leaves state unchanged`() = runRealLauncherScenario {
        val state = stateHolder.state
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        presenter.present()

        registry.launches.expectNoEvents()
        analytics.launches.expectNoEvents()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(stateHolder.state).isEqualTo(state)
    }

    @Test
    fun `Link launch failure clears open state and allows retry`() = runRealLauncherScenario {
        registry.launchError = IllegalStateException("Cannot launch")

        presenter.present()

        registry.launches.awaitItem()
        analytics.launches.expectNoEvents()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        registry.launchError = null
        presenter.present()
        val args = registry.launches.awaitItem() as LinkActivityContract.Args
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.PaymentMethodSelection(selectedPayment.details))
        analytics.launches.awaitItem()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `other Link launch exceptions propagate`() = runRealLauncherScenario {
        val error = IllegalArgumentException("Unexpected launch error")
        registry.launchError = error

        val result = runCatching { presenter.present() }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
        registry.launches.awaitItem()
    }

    @Test
    fun `successful real Link presentation preserves open state after destruction`() = runRealLauncherScenario {
        presenter.present()
        registry.launches.awaitItem()
        analytics.launches.awaitItem()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(SheetStateHolder(savedStateHandle).sheetIsOpen).isTrue()
    }

    @Suppress("LongMethod")
    private fun runRealLauncherScenario(
        block: suspend RealLauncherScenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create(
                paymentSelection = linkSelection,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    linkState = com.stripe.android.paymentsheet.state.LinkState(
                        configuration = TestFactory.LINK_CONFIGURATION,
                        loginState = com.stripe.android.paymentsheet.state.LinkState.LoginState.LoggedIn,
                        signupMode = null,
                    ),
                ),
            )
        }
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val linkAccountHolder = LinkAccountHolder(savedStateHandle).apply {
            set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        }
        val customerStateHolder = com.stripe.android.paymentsheet.DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = stateHolder.selection,
            customerMetadata = stateFlowOf(null),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val registry = FakeCheckoutActivityResultRegistry()
        val analytics = FakeCheckoutLinkAnalyticsHelper()
        val linkStore = FakeLinkStore()
        val errorReporter = FakeErrorReporter()
        val linkGateFactory = FakeLinkGate.Factory()
        val launcher = LinkPaymentLauncher(
            linkAnalyticsComponentFactory = object : LinkAnalyticsComponent.Factory {
                override fun create() = object : LinkAnalyticsComponent {
                    override val linkAnalyticsHelper = analytics
                }
            },
            paymentElementCallbackIdentifier = "CheckoutTest",
            linkActivityContract = LinkActivityContract(
                nativeLinkActivityContract = NativeLinkActivityContract("CheckoutTest", RequestSurface.PaymentElement),
                webLinkActivityContract = WebLinkActivityContract(object : AbsFakeStripeRepository() {}, errorReporter),
                linkGateFactory = linkGateFactory,
            ),
            linkStore = linkStore,
        )
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val presenter = CheckoutLinkPaymentOptionsPresenter(
            defaultPresenter = DefaultEmbeddedPaymentOptionsPresenter(
                state = stateFlowOf(null),
                sheetStateHolder = sheetStateHolder,
                customerStateHolder = customerStateHolder,
                selectionHolder = stateHolder,
                errorReporter = errorReporter,
            ),
            selectionLauncher = LinkPaymentMethodSelectionLauncher(launcher, linkGateFactory, linkAccountHolder, 123),
            linkPaymentLauncher = launcher,
            activityResultRegistry = registry,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            sheetStateHolder = sheetStateHolder,
        )
        RealLauncherScenario(
            presenter, registry, analytics, lifecycleOwner, stateHolder, sheetStateHolder, savedStateHandle,
        ).block()
        // A default presentation would report an unconfigured state.
        assertThat(errorReporter.getLoggedErrors()).isEmpty()
        registry.launches.ensureAllEventsConsumed()
        analytics.ensureAllEventsConsumed()
        linkStore.ensureAllEventsConsumed()
    }

    private data class RealLauncherScenario(
        val presenter: CheckoutLinkPaymentOptionsPresenter,
        val registry: FakeCheckoutActivityResultRegistry,
        val analytics: FakeCheckoutLinkAnalyticsHelper,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val sheetStateHolder: SheetStateHolder,
        val savedStateHandle: SavedStateHandle,
    )

    @Suppress("LongMethod")
    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        selection: PaymentSelection? = linkSelection,
        block: Scenario.() -> Unit,
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = com.stripe.android.paymentsheet.state.LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = com.stripe.android.paymentsheet.state.LinkState.LoginState.LoggedIn,
                signupMode = null,
            )
        )
        val existingState = savedStateHandle.get<CheckoutControllerState>(CheckoutControllerStateHolder.STATE_KEY)
        val state = existingState ?: CheckoutControllerStateFactory.create(
            paymentSelection = selection,
            paymentMethodMetadata = paymentMethodMetadata,
        )
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            this.state = state
        }
        val customerState = MutableStateFlow<CustomerState?>(null)
        val customerStateHolder = mock<CustomerStateHolder> {
            on { customer } doReturn customerState
        }
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply { set(linkAccountInfo) }
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val defaultPresenter = mock<DefaultEmbeddedPaymentOptionsPresenter>()
        val linkPaymentLauncher = mock<LinkPaymentLauncher>()
        val activityResultRegistry = mock<ActivityResultRegistry>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val presenter = CheckoutLinkPaymentOptionsPresenter(
            defaultPresenter = defaultPresenter,
            selectionLauncher = LinkPaymentMethodSelectionLauncher(
                launcher = linkPaymentLauncher,
                linkGateFactory = FakeLinkGate.Factory(),
                linkAccountHolder = linkAccountHolder,
                statusBarColor = 123,
            ),
            linkPaymentLauncher = linkPaymentLauncher,
            activityResultRegistry = activityResultRegistry,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            sheetStateHolder = sheetStateHolder,
        )
        val callbackCaptor = argumentCaptor<(LinkActivityResult) -> Unit>()
        verify(linkPaymentLauncher).register(
            key = eq(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER),
            activityResultRegistry = same(activityResultRegistry),
            callback = callbackCaptor.capture(),
        )

        Scenario(
            presenter = presenter,
            defaultPresenter = defaultPresenter,
            linkPaymentLauncher = linkPaymentLauncher,
            activityResultRegistry = activityResultRegistry,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerState = customerState,
            sheetStateHolder = sheetStateHolder,
            linkAccountHolder = linkAccountHolder,
            linkAccountInfo = linkAccountInfo,
            paymentMethodMetadata = paymentMethodMetadata,
            resultCallback = callbackCaptor.firstValue,
        ).block()
    }

    private data class Scenario(
        val presenter: CheckoutLinkPaymentOptionsPresenter,
        val defaultPresenter: DefaultEmbeddedPaymentOptionsPresenter,
        val linkPaymentLauncher: LinkPaymentLauncher,
        val activityResultRegistry: ActivityResultRegistry,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val customerState: MutableStateFlow<CustomerState?>,
        val sheetStateHolder: SheetStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val paymentMethodMetadata: com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata,
        val resultCallback: (LinkActivityResult) -> Unit,
    ) {
        val linkConfiguration: LinkConfiguration
            get() = requireNotNull(paymentMethodMetadata.linkState?.configuration)

        fun verifyLinkWasNotPresented() {
            verify(linkPaymentLauncher, never()).present(
                configuration = any(),
                paymentMethodMetadata = any(),
                linkAccountInfo = any(),
                launchMode = any(),
                linkExpressMode = any(),
                statusBarColor = anyOrNull(),
            )
        }
    }

    private companion object {
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        )
        val linkSelection = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = selectedPayment,
        )
    }
}

private class FakeCheckoutActivityResultRegistry : ActivityResultRegistry() {
    var launchError: Throwable? = null
    val launches = Turbine<Any?>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        launches.add(input)
        launchError?.let { throw it }
    }
}

private class FakeCheckoutLinkAnalyticsHelper : LinkAnalyticsHelper {
    val launches = Turbine<Unit>()
    private val results = Turbine<LinkActivityResult>()
    private val popupSkips = Turbine<Unit>()

    override fun onLinkLaunched() = launches.add(Unit)
    override fun onLinkResult(linkActivityResult: LinkActivityResult) = results.add(linkActivityResult)
    override fun onLinkPopupSkipped() = popupSkips.add(Unit)

    fun ensureAllEventsConsumed() {
        launches.ensureAllEventsConsumed()
        results.ensureAllEventsConsumed()
        popupSkips.ensureAllEventsConsumed()
    }
}
