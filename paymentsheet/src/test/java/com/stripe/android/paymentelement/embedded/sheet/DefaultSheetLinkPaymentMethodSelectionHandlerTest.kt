package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.effectiveLinkBrand
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import javax.inject.Provider

internal class DefaultSheetLinkPaymentMethodSelectionHandlerTest {

    @Test
    fun `launchIfEligible presents Link payment method selection for payment options`() = runScenario {
        val launched = handler.launchIfEligible(LINK_SELECTION)

        assertThat(launched).isTrue()
        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNull()
        assertThat(linkLauncher.presentCalls.awaitItem()).isEqualTo(
            RecordingLinkPaymentLauncher.PresentCall(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = LINK_ACCOUNT_INFO,
                launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null),
                linkExpressMode = LinkExpressMode.ENABLED,
                statusBarColor = null,
            )
        )
    }

    @Test
    fun `launchIfEligible does not launch for non-Link selection`() = runScenario {
        assertThat(handler.launchIfEligible(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)).isFalse()
    }

    @Test
    fun `launchIfEligible does not launch for null selection`() = runScenario {
        assertThat(handler.launchIfEligible(null)).isFalse()
    }

    @Test
    fun `launchIfEligible does not launch when RUX in FlowController is disabled`() = runScenario(
        showRuxInFlowController = false,
    ) {
        assertThat(handler.launchIfEligible(LINK_SELECTION)).isFalse()
    }

    @Test
    fun `launchIfEligible does not launch without Link state`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(linkState = null),
    ) {
        assertThat(handler.launchIfEligible(LINK_SELECTION)).isFalse()
    }

    @Test
    fun `launchIfEligible does not launch in form mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
    ) {
        assertThat(handler.launchIfEligible(LINK_SELECTION)).isFalse()
    }

    @Test
    fun `launchIfEligible does not launch in manage mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Manage,
    ) {
        assertThat(handler.launchIfEligible(LINK_SELECTION)).isFalse()
    }

    @Test
    fun `register does not register in form mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
    ) {
        handler.register(
            activityResultCaller = mock<ActivityResultCaller>(),
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
        )
    }

    @Test
    fun `register unregisters Link launcher on destroy`() = runScenario {
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val activityResultCaller = mock<ActivityResultCaller>()

        handler.register(activityResultCaller, lifecycleOwner)

        assertThat(linkLauncher.registerCalls.awaitItem().activityResultCaller).isEqualTo(activityResultCaller)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(linkLauncher.unregisterCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `Link result Canceled does nothing`() = runScenario {
        val onLinkResult = registerAndAwaitCallback()

        onLinkResult(LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None))

        stateHolder.updateErrorTurbine.expectNoEvents()
        assertThat(selectionHolder.selection.value).isNull()
    }

    @Test
    fun `Link result Failed sets error`() = runScenario {
        val onLinkResult = registerAndAwaitCallback()
        val error = Exception("link failed")

        onLinkResult(LinkActivityResult.Failed(error = error, linkAccountUpdate = LinkAccountUpdate.None))

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isEqualTo(error.stripeErrorMessage())
    }

    @Test
    fun `Link result PaymentMethodObtained sets error`() = runScenario {
        val onLinkResult = registerAndAwaitCallback()

        onLinkResult(LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Link result Completed sets Link selection and completes selection`() = runScenario(
        linkAccountInfo = LinkAccountUpdate.Value(null),
    ) {
        val onLinkResult = registerAndAwaitCallback()
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        )

        onLinkResult(
            LinkActivityResult.Completed(
                linkAccountUpdate = LINK_ACCOUNT_INFO,
                selectedPayment = selectedPayment,
            )
        )

        assertThat(continueCoordinator.completeSelectionCalls.awaitItem()).isEqualTo(Unit)
        assertThat(selectionHolder.selection.value).isEqualTo(
            PaymentSelection.Link(
                brand = TestFactory.LINK_CONFIGURATION.effectiveLinkBrand(TestFactory.LINK_ACCOUNT),
                selectedPayment = selectedPayment,
            )
        )
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(LINK_ACCOUNT_INFO)
    }

    private suspend fun Scenario.registerAndAwaitCallback(): (LinkActivityResult) -> Unit {
        handler.register(
            activityResultCaller = mock<ActivityResultCaller>(),
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
        )
        return linkLauncher.registerCalls.awaitItem().callback
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            ),
        ),
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions,
        showRuxInFlowController: Boolean = true,
        linkAccountInfo: LinkAccountUpdate.Value = LINK_ACCOUNT_INFO,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        RecordingLinkPaymentLauncher.test {
            val linkGate = FakeLinkGate().apply { setShowRuxInFlowController(showRuxInFlowController) }
            val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply { set(linkAccountInfo) }
            val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
            val stateHolder = FakeSheetActivityStateHolder()
            val continueCoordinator = FakeSheetActivityContinueCoordinator()
            val handler = DefaultSheetLinkPaymentMethodSelectionHandler(
                linkPaymentLauncher = launcher,
                linkGateFactory = FakeLinkGate.Factory(linkGate),
                paymentMethodMetadata = paymentMethodMetadata,
                launchMode = launchMode,
                linkAccountHolder = linkAccountHolder,
                selectionHolder = selectionHolder,
                stateHolder = stateHolder,
                continueCoordinatorProvider = Provider { continueCoordinator },
            )

            Scenario(
                handler = handler,
                linkLauncher = this,
                stateHolder = stateHolder,
                continueCoordinator = continueCoordinator,
                selectionHolder = selectionHolder,
                linkAccountHolder = linkAccountHolder,
                paymentMethodMetadata = paymentMethodMetadata,
            ).block()

            stateHolder.validate()
            continueCoordinator.validate()
        }
    }

    private data class Scenario(
        val handler: DefaultSheetLinkPaymentMethodSelectionHandler,
        val linkLauncher: RecordingLinkPaymentLauncher.Scenario,
        val stateHolder: FakeSheetActivityStateHolder,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
        val selectionHolder: EmbeddedSelectionHolder,
        val linkAccountHolder: LinkAccountHolder,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

    private companion object {
        val LINK_SELECTION = PaymentSelection.Link(brand = LinkBrand.Link)
        val LINK_ACCOUNT_INFO = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
    }
}
