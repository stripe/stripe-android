package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCallback
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.asCallbackFor
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfirmationPerformerTest {

    @Test
    fun `confirm does nothing when state is not loaded`() = runScenario(state = null) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when there is no selection`() = runScenario(
        state = CheckoutControllerStateFactory.create(paymentSelection = null),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when the selection cannot be converted to a confirmation option`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            paymentSelection = PaymentSelection.GooglePay,
        ),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() = runScenario(
        statusBarColor = STATUS_BAR_COLOR,
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
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
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    @Test
    fun `confirm shows mandate before confirming an unacknowledged saved SEPA payment method`() = runScenario(
        state = savedSepaState(),
    ) {
        performer.confirm()

        val args = activityResultCallerScenario.awaitLaunchCall() as SepaMandateContract.Args
        assertThat(args.merchantName).isEqualTo(stateHolder.state?.checkoutSessionResponse?.businessName)
        confirmationHandler.startTurbine.expectNoEvents()
    }

    @Test
    fun `confirm starts confirmation after saved SEPA mandate is acknowledged`() = runScenario(
        state = savedSepaState(),
    ) {
        performer.confirm()
        activityResultCallerScenario.awaitLaunchCall()

        sepaMandateCallback.onActivityResult(SepaMandateResult.Acknowledged)

        confirmationHandler.startTurbine.awaitItem()
        assertThat(stateHolder.state?.paymentSelection?.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `confirm reports cancellation when saved SEPA mandate is canceled`() = runScenario(
        state = savedSepaState(),
    ) {
        performer.confirm()
        activityResultCallerScenario.awaitLaunchCall()

        sepaMandateCallback.onActivityResult(SepaMandateResult.Canceled)

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
        confirmationHandler.startTurbine.expectNoEvents()
    }

    @Test
    fun `confirm directly confirms an acknowledged saved SEPA payment method`() {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD).also {
            it.hasAcknowledgedSepaMandate = true
        }
        runScenario(state = savedSepaState(paymentSelection)) {
            performer.confirm()

            confirmationHandler.startTurbine.awaitItem()
        }
    }

    @Test
    fun `confirm directly confirms saved SEPA when merchant displays mandate text`() = runScenario(
        state = savedSepaState(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(false)
                .build(),
        ),
    ) {
        performer.confirm()

        confirmationHandler.startTurbine.awaitItem()
    }

    @Test
    fun `confirm records the payment selection for analytics`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(eventReporter.paymentSuccessCalls.awaitItem().paymentSelection)
            .isEqualTo(PaymentSelection.GooglePay)
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
        paymentSelection: PaymentSelection.Saved =
            PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        embeddedConfiguration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = paymentSelection,
            embeddedConfiguration = embeddedConfiguration,
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        statusBarColor: Int? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        DummyActivityResultCaller.test {
            val confirmationHandler = FakeConfirmationHandler()
            val savedStateHandle = SavedStateHandle()
            val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
            stateHolder.state = state
            val sessionRefresher = FakeCheckoutSessionRefresher()
            val resultTurbine = Turbine<CheckoutController.Result>()
            val resultCallback = CheckoutController.ResultCallback(resultTurbine::add)
            val operationCoordinator = CheckoutOperationCoordinator(
                confirmationHandler = confirmationHandler,
                sheetStateHolder = SheetStateHolder(savedStateHandle),
                sessionRefresher = sessionRefresher,
                logger = Logger.noop(),
                resultCallback = resultCallback,
            )
            val eventReporter = FakeEventReporter()
            val analyticsPerformer = CheckoutAnalyticsPerformer(
                confirmationHandler = confirmationHandler,
                eventReporter = eventReporter,
                savedStateHandle = savedStateHandle,
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
                activityResultCaller = activityResultCaller,
                lifecycleOwner = TestLifecycleOwner(),
                resultCallback = resultCallback,
                statusBarColor = statusBarColor,
                viewModelScope = backgroundScope,
            )
            awaitNextRegisteredLauncher()
            val sepaMandateCallback = awaitRegisterCall().callback.asCallbackFor<SepaMandateResult>()

            Scenario(
                performer = performer,
                confirmationHandler = confirmationHandler,
                eventReporter = eventReporter,
                stateHolder = stateHolder,
                activityResultCallerScenario = this,
                sepaMandateCallback = sepaMandateCallback,
                resultTurbine = resultTurbine,
            ).block()

            confirmationHandler.validate()
            sessionRefresher.ensureAllEventsConsumed()
            eventReporter.validate()
            resultTurbine.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val performer: CheckoutConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeEventReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val activityResultCallerScenario: DummyActivityResultCaller.Scenario,
        val sepaMandateCallback: ActivityResultCallback<SepaMandateResult>,
        val resultTurbine: Turbine<CheckoutController.Result>,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 0x00FF00
    }
}
