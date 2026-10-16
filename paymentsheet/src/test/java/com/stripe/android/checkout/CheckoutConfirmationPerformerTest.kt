package com.stripe.android.checkout

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.Test
import kotlin.time.TestTimeSource

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfirmationPerformerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @Test
    fun `presenter confirm from a background thread resolves the performer on Main and starts confirmation`() =
        runScenario(state = googlePayState(paymentSelection = PaymentSelection.GooglePay)) {
            withContext(Dispatchers.IO) {
                assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
                presenter.confirm()
            }

            assertThat(performerProvider.getCalls.awaitItem()).isSameInstanceAs(Looper.getMainLooper().thread)
            assertThat(confirmationHandler.startTurbine.awaitItem().confirmationOption)
                .isInstanceOf<GooglePayConfirmationOption>()
            awaitAnalyticsEvent("checkout_elements.confirm.started")
        }

    @Test
    fun `confirm fails when state is not loaded`() = runScenario(state = null) {
        performer.confirm()
        assertRejection("Cannot confirm checkout before it is configured.")
        assertThat(operationCoordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirm fails when there is no selection`() = runScenario(
        state = CheckoutControllerStateFactory.create(paymentSelection = null),
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout without a selected payment option.")
        assertThat(operationCoordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirm fails when the selection cannot be converted to a confirmation option`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            paymentSelection = PaymentSelection.GooglePay,
        ),
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout because the selected payment option cannot be confirmed.")
        assertThat(operationCoordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `presenter confirm from background thread delivers rejection on Main`() = runScenario(state = null) {
        withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            presenter.confirm()
        }

        assertThat(performerProvider.getCalls.awaitItem()).isSameInstanceAs(Looper.getMainLooper().thread)
        assertRejection("Cannot confirm checkout before it is configured.")
    }

    @Test
    fun `confirm checks sheet before validating unloaded state`() = runScenario(
        state = null,
        sheetIsOpen = true,
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout while a payment flow is presented.")
    }

    @Test
    fun `confirm can retry after state becomes available`() = runScenario(state = null) {
        performer.confirm()
        assertRejection("Cannot confirm checkout before it is configured.")

        stateHolder.state = googlePayState(paymentSelection = PaymentSelection.GooglePay)
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
    }

    @Test
    fun `confirm can retry after a payment option is selected`() = runScenario(
        state = googlePayState(paymentSelection = null),
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout without a selected payment option.")

        stateHolder.state = googlePayState(paymentSelection = PaymentSelection.GooglePay)
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
    }

    @Test
    fun `confirm can retry after the payment option becomes confirmable`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            paymentSelection = PaymentSelection.GooglePay,
        ),
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout because the selected payment option cannot be confirmed.")

        stateHolder.state = googlePayState(paymentSelection = PaymentSelection.GooglePay)
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
    }

    @Test
    fun `rejected confirmation does not record confirmation analytics`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
        sheetIsOpen = true,
    ) {
        performer.confirm()
        assertRejection("Cannot confirm checkout while a payment flow is presented.")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        testScheduler.runCurrent()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
    }

    @Test
    fun `confirm checks mutations before validating selection and can retry after mutation`() = runScenario(
        state = googlePayState(paymentSelection = null),
    ) {
        val releaseMutation = CompletableDeferred<Unit>()
        val mutation = async {
            operationCoordinator.runMutation {
                releaseMutation.await()
                Result.success(Unit)
            }
        }
        testScheduler.runCurrent()

        performer.confirm()
        assertRejection("Cannot confirm checkout while checkout mutations are in progress.")
        assertThat(operationCoordinator.isUpdating.value).isTrue()

        stateHolder.state = googlePayState(paymentSelection = PaymentSelection.GooglePay)
        releaseMutation.complete(Unit)
        mutation.await()
        testScheduler.runCurrent()
        assertThat(operationCoordinator.isUpdating.value).isFalse()
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
    }

    @Test
    fun `rejected repeat confirmation preserves the original payment selection analytics`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        val started = awaitAnalyticsEvent("checkout_elements.confirm.started")
        stateHolder.state = linkState()

        performer.confirm()
        assertRejection("Cannot confirm checkout while another confirmation is in progress.")
        assertThat(operationCoordinator.isUpdating.value).isTrue()

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        val terminal = awaitAnalyticsEvent("checkout_elements.confirm.succeeded")
        assertThat(started["event"]).isEqualTo("checkout_elements.confirm.started")
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(terminal).containsEntry("selected_lpm", "google_pay")
        assertThat(terminal).containsEntry("invocation_source", "merchant")
        assertThat(terminal).containsEntry(
            "checkout_session_id",
            requireNotNull(stateHolder.state).checkoutSessionResponse.id,
        )
    }

    @Test
    fun `confirm preserves Google Pay billing email when Checkout has billing defaults`() = runScenario(
        statusBarColor = STATUS_BAR_COLOR,
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay).let { state ->
            val response = state.checkoutSessionResponse.copy(customerEmail = "checkout@example.com")
            state.copy(
                checkoutSessionResponse = response,
                paymentMethodMetadata = state.paymentMethodMetadata.copy(
                    integrationMetadata = IntegrationMetadata.CheckoutSession(
                        id = response.id,
                        instancesKey = "test",
                        checkoutSessionResponse = response,
                        collectedEmail = null,
                    ),
                ),
            )
        },
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
        assertThat((args.confirmationOption as GooglePayConfirmationOption).config.billingEmailOverride).isNull()
        assertThat(args.paymentMethodMetadata)
            .isEqualTo(stateHolder.state?.paymentMethodMetadata)
        assertThat(args.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `confirm starts confirmation with a Link option`() = runScenario(
        state = linkState(),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        awaitAnalyticsEvent("checkout_elements.confirm.started")
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    @Test
    fun `confirm keeps saved SEPA unacknowledged when embedded view displays mandate text`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = true),
    ) {
        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        awaitAnalyticsEvent("checkout_elements.confirm.started")
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `confirm acknowledges saved SEPA when embedded view omits mandate text`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        awaitAnalyticsEvent("checkout_elements.confirm.started")
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `confirm records the payment selection for analytics`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        val started = awaitAnalyticsEvent("checkout_elements.confirm.started")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        val terminal = awaitAnalyticsEvent("checkout_elements.confirm.succeeded")
        assertThat(listOf(started["event"], terminal["event"])).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        assertThat(terminal).containsEntry("selected_lpm", "google_pay")
        assertThat(terminal).containsEntry("invocation_source", "merchant")
        assertThat(terminal).containsEntry(
            "checkout_session_id",
            requireNotNull(stateHolder.state).checkoutSessionResponse.id,
        )
    }

    private fun googlePayState(
        paymentSelection: PaymentSelection?,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = paymentSelection,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
        )
    }

    private fun linkState(): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Link(brand = LinkBrand.Link),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        )
    }

    private fun savedSepaState(
        embeddedViewDisplaysMandateText: Boolean,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(embeddedViewDisplaysMandateText)
                .build(),
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        )
    }

    @Suppress("LongMethod")
    private fun runScenario(
        state: CheckoutControllerState?,
        statusBarColor: Int? = null,
        sheetIsOpen: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.state = state
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val results = Turbine<CheckoutController.Result>()
        val resultThreads = Turbine<Thread>()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle).apply { this.sheetIsOpen = sheetIsOpen },
            sessionRefresher = sessionRefresher,
            logger = Logger.noop(),
            resultCallback = {
                results.add(it)
                resultThreads.add(Thread.currentThread())
            },
            viewModelScope = backgroundScope,
        )
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val apiConfigurationProvider = Provider { PaymentMethodMetadataFactory.create().apiConfiguration }
        val analyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = LinkAccountHolder(savedStateHandle),
            context = ApplicationProvider.getApplicationContext<Context>(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            apiConfigurationProvider = apiConfigurationProvider,
            timeSource = TestTimeSource(),
            workContext = UnconfinedTestDispatcher(testScheduler),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            analyticsPerformer.reportConfirmationResults()
        }
        val performer = CheckoutConfirmationPerformer(
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
            operationCoordinator = operationCoordinator,
            analyticsPerformer = analyticsPerformer,
            commonConfigurationFactory = CheckoutCommonConfigurationFactory(appName = "Test App"),
            statusBarColor = statusBarColor,
            viewModelScope = backgroundScope,
        )

        val performerProvider = FakeConfirmationPerformerProvider(performer)
        val presenter = createPresenter(performerProvider, backgroundScope)

        Scenario(
            presenter = presenter,
            performerProvider = performerProvider,
            performer = performer,
            confirmationHandler = confirmationHandler,
            analyticsRequestExecutor = analyticsRequestExecutor,
            stateHolder = stateHolder,
            operationCoordinator = operationCoordinator,
            results = results,
            resultThreads = resultThreads,
            sessionRefresher = sessionRefresher,
            testScope = this,
        ).block()

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        analyticsRequestExecutor.validate()
        results.ensureAllEventsConsumed()
        resultThreads.ensureAllEventsConsumed()
        performerProvider.getCalls.ensureAllEventsConsumed()
        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private fun createPresenter(
        performerProvider: Provider<CheckoutConfirmationPerformer>,
        coroutineScope: CoroutineScope,
    ): CheckoutPresenter {
        return CheckoutPresenter(
            paymentElementProvider = Lazy { error("Unexpected payment element access") },
            currencySelectorElementProvider = Lazy { error("Unexpected currency selector access") },
            shippingAddressElementProvider = Lazy { error("Unexpected shipping address element access") },
            expressCheckoutElementProvider = Lazy { error("Unexpected express checkout element access") },
            checkoutConfirmationPerformerProvider = performerProvider,
            coroutineScope = coroutineScope,
        )
    }

    private class FakeConfirmationPerformerProvider(
        private val performer: CheckoutConfirmationPerformer,
    ) : Provider<CheckoutConfirmationPerformer> {
        val getCalls = Turbine<Thread>()

        override fun get(): CheckoutConfirmationPerformer {
            getCalls.add(Thread.currentThread())
            return performer
        }
    }

    private class Scenario(
        val presenter: CheckoutPresenter,
        val performerProvider: FakeConfirmationPerformerProvider,
        val performer: CheckoutConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val stateHolder: CheckoutControllerStateHolder,
        val operationCoordinator: CheckoutOperationCoordinator,
        val results: Turbine<CheckoutController.Result>,
        val resultThreads: Turbine<Thread>,
        val sessionRefresher: FakeCheckoutSessionRefresher,
        private val testScope: TestScope,
    ) : CoroutineScope by testScope {
        val testScheduler = testScope.testScheduler

        suspend fun awaitAnalyticsEvent(expectedEvent: String): Map<String, *> {
            val params = analyticsRequestExecutor.requestTurbine.awaitItem().params
            assertThat(params["event"]).isEqualTo(expectedEvent)
            return params
        }

        suspend fun assertRejection(message: String) {
            testScheduler.runCurrent()
            val result = results.awaitItem()
            assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
            val error = (result as CheckoutController.Result.Failed).error
            assertThat(error).isInstanceOf<IllegalStateException>()
            assertThat(error).hasMessageThat().isEqualTo(message)
            assertThat(resultThreads.awaitItem()).isSameInstanceAs(Looper.getMainLooper().thread)
            results.expectNoEvents()
            confirmationHandler.startTurbine.expectNoEvents()
            analyticsRequestExecutor.requestTurbine.expectNoEvents()
            sessionRefresher.calls.expectNoEvents()
        }
    }

    private companion object {
        const val STATUS_BAR_COLOR = 0x00FF00
    }
}
