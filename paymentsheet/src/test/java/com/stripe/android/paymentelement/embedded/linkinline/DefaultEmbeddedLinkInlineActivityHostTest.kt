package com.stripe.android.paymentelement.embedded.linkinline

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.FakeCheckoutLinkAnalyticsHelper
import com.stripe.android.checkout.FakeLinkActivityResultRegistry
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.NativeLinkActivityContract
import com.stripe.android.link.TestFactory
import com.stripe.android.link.WebLinkActivityContract
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeLinkStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedLinkInlineActivityHostTest {

    @Test
    fun `wallet opens to pick what to pay with`() = runScenario {
        val linkConfiguration = TestFactory.LINK_CONFIGURATION
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        host.launchLinkWallet(
            configuration = linkConfiguration,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkAccountInfo = linkAccountInfo,
            selectedPayment = PICK,
        )

        linkAnalyticsHelper.launchedCalls.awaitItem()
        val args = activityResultRegistry.launchCalls.awaitItem().args
        assertThat(args.configuration).isEqualTo(linkConfiguration)
        assertThat(args.linkAccountInfo).isEqualTo(linkAccountInfo)
        assertThat(args.launchMode).isEqualTo(LinkLaunchMode.PaymentMethodSelection(selectedPayment = PICK.details))
        assertThat(args.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(args.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `wallet result goes to inline Link verification`() = runScenario {
        host.launchLinkWallet(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            selectedPayment = null,
        )
        linkAnalyticsHelper.launchedCalls.awaitItem()
        val launch = activityResultRegistry.launchCalls.awaitItem()
        val result = LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)

        assertThat(activityResultRegistry.dispatchResult(launch.requestCode, result)).isTrue()

        assertThat(linkAnalyticsHelper.resultCalls.awaitItem()).isSameInstanceAs(result)
        assertThat(linkInlineAuth.onWalletResultCalls.awaitItem()).isSameInstanceAs(result)
    }

    @Test
    fun `wallet launcher is unregistered when the screen is destroyed`() = runScenario {
        host.launchLinkWallet(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            selectedPayment = null,
        )
        linkAnalyticsHelper.launchedCalls.awaitItem()
        val launch = activityResultRegistry.launchCalls.awaitItem()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        // The registry keeps a launched request to deliver its result to the next registration.
        activityResultRegistry.dispatchResult(
            launch.requestCode,
            LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None),
        )

        linkAnalyticsHelper.resultCalls.expectNoEvents()
        linkInlineAuth.onWalletResultCalls.expectNoEvents()
    }

    @Test
    fun `paying in the wallet selects Link and confirms`() = runScenario {
        confirmationStateHolder.state = EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = null,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant").build(),
            statusBarColor = null,
        )

        host.payWithLinkWallet(LinkBrand.Onelink)

        confirmCalls.awaitItem()
        assertThat(confirmationStateHolder.state?.selection).isEqualTo(PaymentSelection.Link(brand = LinkBrand.Onelink))
    }

    @Test
    fun `paying in the wallet waits for configure to finish`() = runScenario {
        host.payWithLinkWallet(LinkBrand.Link)
        confirmCalls.expectNoEvents()

        confirmationStateHolder.state = EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = null,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant").build(),
            statusBarColor = null,
        )

        confirmCalls.awaitItem()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val linkAnalyticsHelper = FakeCheckoutLinkAnalyticsHelper()
        val linkStore = FakeLinkStore()
        val linkPaymentLauncher = LinkPaymentLauncher(
            linkAnalyticsComponentFactory = object : LinkAnalyticsComponent.Factory {
                override fun create() = object : LinkAnalyticsComponent {
                    override val linkAnalyticsHelper = linkAnalyticsHelper
                }
            },
            paymentElementCallbackIdentifier = "embedded_test",
            linkActivityContract = LinkActivityContract(
                nativeLinkActivityContract = NativeLinkActivityContract(
                    paymentElementCallbackIdentifier = "embedded_test",
                    requestSurface = RequestSurface.PaymentElement,
                ),
                webLinkActivityContract = WebLinkActivityContract(FakeStripeRepository(), FakeErrorReporter()),
                linkGateFactory = FakeLinkGate.Factory(),
            ),
            linkStore = linkStore,
        )
        val activityResultRegistry = FakeLinkActivityResultRegistry()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val scope = CoroutineScope(
            backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job])
        )
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = scope,
        )
        val confirmCalls = Turbine<Unit>()
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth()

        val host = DefaultEmbeddedLinkInlineActivityHost(
            activityResultRegistryOwner = object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = activityResultRegistry
            },
            lifecycleOwner = lifecycleOwner,
            linkPaymentLauncher = linkPaymentLauncher,
            linkInlineAuth = linkInlineAuth,
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
            confirmationHelper = object : EmbeddedConfirmationHelper {
                override fun confirm() {
                    confirmCalls.add(Unit)
                }
            },
            statusBarColor = STATUS_BAR_COLOR,
        )

        Scenario(
            host = host,
            activityResultRegistry = activityResultRegistry,
            linkAnalyticsHelper = linkAnalyticsHelper,
            lifecycleOwner = lifecycleOwner,
            confirmationStateHolder = confirmationStateHolder,
            confirmCalls = confirmCalls,
            linkInlineAuth = linkInlineAuth,
        ).block()

        scope.cancel()
        activityResultRegistry.launchCalls.ensureAllEventsConsumed()
        linkAnalyticsHelper.ensureAllEventsConsumed()
        linkStore.ensureAllEventsConsumed()
        confirmCalls.ensureAllEventsConsumed()
        linkInlineAuth.ensureAllEventsConsumed()
    }

    private class Scenario(
        val host: DefaultEmbeddedLinkInlineActivityHost,
        val activityResultRegistry: FakeLinkActivityResultRegistry,
        val linkAnalyticsHelper: FakeCheckoutLinkAnalyticsHelper,
        val lifecycleOwner: TestLifecycleOwner,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val confirmCalls: Turbine<Unit>,
        val linkInlineAuth: FakeEmbeddedLinkInlineAuth,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 123

        val PICK = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        )
    }
}
