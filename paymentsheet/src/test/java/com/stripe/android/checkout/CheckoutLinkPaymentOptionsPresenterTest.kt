@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER
import com.stripe.android.customersheet.FakeStripeRepository
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
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeLinkStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutLinkPaymentOptionsPresenterTest {
    @Test
    fun `registers direct Link with unique key and host registry`() = runScenario {
        val registryState = Bundle()
        activityResultRegistry.onSaveInstanceState(registryState)

        assertThat(registryState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"))
            .containsExactly("${CALLBACK_IDENTIFIER}_$CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER")
    }

    @Test
    fun `ordinary payment options delegate to default presenter`() = runScenario(
        selection = PaymentSelection.GooglePay,
    ) {
        presenter.present()

        assertThat(sheetLauncher.paymentOptionsCalls.awaitItem().paymentMethodMetadata)
            .isEqualTo(paymentMethodMetadata)
        verifyLinkWasNotPresented()
    }

    @Test
    fun `eligible Link launches with exact arguments and keeps sheet open`() = runScenario {
        presenter.present()

        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(activityResultRegistry.launchCalls.awaitItem().args).isEqualTo(
            LinkActivityContract.Args(
                configuration = linkConfiguration,
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = linkAccountInfo,
                launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment.details),
                linkExpressMode = LinkExpressMode.ENABLED,
                statusBarColor = 123,
            )
        )
        linkAnalyticsHelper.launchedCalls.awaitItem()
        sheetLauncher.paymentOptionsCalls.expectNoEvents()
    }

    @Test
    fun `completion updates selection and closes`() = runScenario {
        val launch = presentLink()
        val updatedPayment = selectedPayment.copy(collectedCvc = "123")

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Completed(LinkAccountUpdate.None, selectedPayment = updatedPayment)
        )

        assertThat(stateHolder.state?.paymentSelection)
            .isEqualTo(linkSelection.copy(selectedPayment = updatedPayment))
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `logout uses current customer fallback and opens payment options`() = runScenario {
        val launch = presentLink()
        val fallback = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(fallback)))

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.Saved(fallback))
        val paymentOptionsCall = sheetLauncher.paymentOptionsCalls.awaitItem()
        assertThat(paymentOptionsCall.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(paymentOptionsCall.selection).isEqualTo(PaymentSelection.Saved(fallback))
        assertThat(paymentOptionsCall.customerState?.paymentMethods).containsExactly(fallback)
    }

    @Test
    fun `pay another way opens payment options`() = runScenario {
        val launch = presentLink()

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(sheetLauncher.paymentOptionsCalls.awaitItem().paymentMethodMetadata)
            .isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `back opens payment options when Link has no usable payment`() = runScenario(
        selection = linkSelection.copy(selectedPayment = null),
    ) {
        val launch = presentLink()

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(sheetLauncher.paymentOptionsCalls.awaitItem().paymentMethodMetadata)
            .isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `back closes when Link has a usable payment`() = runScenario {
        val launch = presentLink()

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        sheetLauncher.paymentOptionsCalls.expectNoEvents()
    }

    @Test
    fun `failure closes and applies Link account update`() = runScenario {
        val launch = presentLink()

        dispatchResult(
            launch = launch,
            result = LinkActivityResult.Failed(
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
            val launch = presentLink()
            val verifyingAccount = LinkAccount(
                TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION),
                    currentAuthenticationLevel = null,
                )
            )

            dispatchResult(
                launch = launch,
                result = LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.Value(verifyingAccount),
                )
            )

            assertThat(stateHolder.state?.linkEagerPresentationSuppressed).isTrue()
        }

        runScenario(savedStateHandle = savedStateHandle) {
            sheetStateHolder.sheetIsOpen = false
            presenter.present()

            assertThat(sheetLauncher.paymentOptionsCalls.awaitItem().paymentMethodMetadata)
                .isEqualTo(paymentMethodMetadata)
            verifyLinkWasNotPresented()
        }
    }

    @Test
    fun `destroy unregisters direct Link and preserves open state`() = runScenario {
        val launch = presentLink()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        activityResultRegistry.dispatchResult(
            launch.requestCode,
            LinkActivityResult.Completed(LinkAccountUpdate.None, selectedPayment = selectedPayment),
        )

        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        linkAnalyticsHelper.resultCalls.expectNoEvents()
    }

    @Test
    fun `eligible Link presentation after destruction leaves state unchanged`() = runScenario {
        val state = stateHolder.state
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        presenter.present()

        verifyLinkWasNotPresented()
        sheetLauncher.paymentOptionsCalls.expectNoEvents()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(stateHolder.state).isEqualTo(state)
    }

    @Suppress("LongMethod")
    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        selection: PaymentSelection? = linkSelection,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
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
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = stateHolder.selection,
            customerMetadata = stateFlowOf(null),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        )
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply { set(linkAccountInfo) }
        val sheetLauncher = FakePaymentOptionsSheetLauncher()
        val sheetStateHolder = SheetStateHolder(savedStateHandle).apply { this.sheetLauncher = sheetLauncher }
        val defaultPresenter = DefaultEmbeddedPaymentOptionsPresenter(
            state = stateFlowOf(
                EmbeddedContentHelperStateFactory.create(paymentMethodMetadata = state.paymentMethodMetadata)
            ),
            sheetStateHolder = sheetStateHolder,
            customerStateHolder = customerStateHolder,
            selectionHolder = stateHolder,
            errorReporter = FakeErrorReporter(),
        )
        val linkAnalyticsHelper = FakeCheckoutLinkAnalyticsHelper()
        val linkStore = FakeLinkStore()
        val linkPaymentLauncher = LinkPaymentLauncher(
            linkAnalyticsComponentFactory = object : LinkAnalyticsComponent.Factory {
                override fun create() = object : LinkAnalyticsComponent {
                    override val linkAnalyticsHelper = linkAnalyticsHelper
                }
            },
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            linkActivityContract = LinkActivityContract(
                nativeLinkActivityContract = NativeLinkActivityContract(
                    paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
                    requestSurface = RequestSurface.PaymentElement,
                ),
                webLinkActivityContract = WebLinkActivityContract(FakeStripeRepository(), FakeErrorReporter()),
                linkGateFactory = FakeLinkGate.Factory(),
            ),
            linkStore = linkStore,
        )
        val activityResultRegistry = FakeLinkActivityResultRegistry()
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

        Scenario(
            presenter = presenter,
            sheetLauncher = sheetLauncher,
            activityResultRegistry = activityResultRegistry,
            linkAnalyticsHelper = linkAnalyticsHelper,
            linkStore = linkStore,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            sheetStateHolder = sheetStateHolder,
            linkAccountHolder = linkAccountHolder,
            linkAccountInfo = linkAccountInfo,
            paymentMethodMetadata = state.paymentMethodMetadata,
        ).block()

        activityResultRegistry.launchCalls.ensureAllEventsConsumed()
        sheetLauncher.paymentOptionsCalls.ensureAllEventsConsumed()
        linkAnalyticsHelper.ensureAllEventsConsumed()
        linkStore.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val presenter: CheckoutLinkPaymentOptionsPresenter,
        val sheetLauncher: FakePaymentOptionsSheetLauncher,
        val activityResultRegistry: FakeLinkActivityResultRegistry,
        val linkAnalyticsHelper: FakeCheckoutLinkAnalyticsHelper,
        val linkStore: FakeLinkStore,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val customerStateHolder: DefaultCustomerStateHolder,
        val sheetStateHolder: SheetStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        val linkConfiguration: LinkConfiguration
            get() = requireNotNull(paymentMethodMetadata.linkState?.configuration)

        suspend fun presentLink(): FakeLinkActivityResultRegistry.LaunchCall {
            presenter.present()
            linkAnalyticsHelper.launchedCalls.awaitItem()
            return activityResultRegistry.launchCalls.awaitItem()
        }

        suspend fun dispatchResult(launch: FakeLinkActivityResultRegistry.LaunchCall, result: LinkActivityResult) {
            assertThat(activityResultRegistry.dispatchResult(launch.requestCode, result)).isTrue()
            assertThat(linkAnalyticsHelper.resultCalls.awaitItem()).isSameInstanceAs(result)
            if (result is LinkActivityResult.Completed) {
                linkStore.markAsUsedCalls.awaitItem()
            }
        }

        fun verifyLinkWasNotPresented() {
            activityResultRegistry.launchCalls.expectNoEvents()
            linkAnalyticsHelper.launchedCalls.expectNoEvents()
        }
    }

    private companion object {
        const val CALLBACK_IDENTIFIER = "checkout_test"
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

internal class FakeLinkActivityResultRegistry : ActivityResultRegistry() {
    val launchCalls = Turbine<LaunchCall>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        launchCalls.add(LaunchCall(requestCode, input as LinkActivityContract.Args))
    }

    data class LaunchCall(val requestCode: Int, val args: LinkActivityContract.Args)
}

internal class FakeCheckoutLinkAnalyticsHelper : LinkAnalyticsHelper {
    val launchedCalls = Turbine<Unit>()
    val resultCalls = Turbine<LinkActivityResult>()

    override fun onLinkLaunched() {
        launchedCalls.add(Unit)
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult) {
        resultCalls.add(linkActivityResult)
    }

    override fun onLinkPopupSkipped() = error("Not expected")

    fun ensureAllEventsConsumed() {
        launchedCalls.ensureAllEventsConsumed()
        resultCalls.ensureAllEventsConsumed()
    }
}

internal class FakePaymentOptionsSheetLauncher : EmbeddedSheetLauncher {
    val paymentOptionsCalls = Turbine<PaymentOptionsCall>()

    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) = error("Not expected")

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) = error("Not expected")

    override fun launchPaymentOptions(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        paymentOptionsCalls.add(PaymentOptionsCall(paymentMethodMetadata, customerState, selection, configuration))
    }

    data class PaymentOptionsCall(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customerState: CustomerState?,
        val selection: PaymentSelection?,
        val configuration: EmbeddedPaymentElement.Configuration?,
    )
}
