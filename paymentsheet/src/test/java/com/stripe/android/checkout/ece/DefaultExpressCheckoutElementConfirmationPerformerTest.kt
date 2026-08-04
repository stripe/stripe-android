package com.stripe.android.checkout.ece

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultExpressCheckoutElementConfirmationPerformerTest {

    @Test
    fun `confirm reports unexpected error when state is not loaded`() = runScenario(
        state = null,
        expressButton = createGooglePayExpressButton(),
    ) {
        performer.confirm(expressButton)

        val call = errorReporter.awaitCall()

        assertThat(call.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_STATE_ON_CONFIRM)
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()
    }

    @Test
    fun `confirm reports unexpected error when confirmation args are null`() = runScenario(
        state = CheckoutControllerStateFactory.create(),
        expressButton = createGooglePayExpressButton(),
    ) {
        performer.confirm(expressButton)

        val call = errorReporter.awaitCall()

        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_CONFIRMATION_ARGS_ON_CONFIRM
        )
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() {
        val state = googlePayState()

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = state.paymentMethodMetadata,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
            assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        }
    }

    @Test
    fun `confirm starts confirmation with a Link option`() {
        val state = CheckoutControllerStateFactory.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        )

        runScenario(
            state = state,
            expressButton = ExpressButton.Link.create(
                paymentMethodMetadata = state.paymentMethodMetadata,
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
            assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        }
    }

    @Test
    fun `confirm reports ECE payment success when confirmation succeeds`() = runScenario(
        state = googlePayState(),
        expressButton = createGooglePayExpressButton(),
    ) {
        confirmationHandler.awaitResultTurbine.add(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        performer.confirm(expressButton)

        confirmationHandler.startTurbine.awaitItem()
        assertThat(eventReporter.calls.awaitItem())
            .isEqualTo(FakeExpressCheckoutElementEventReporter.Call.OnEcePaymentSuccess(expressButton))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `confirm reports ECE payment failure when confirmation fails`() = runScenario(
        state = googlePayState(),
        expressButton = createGooglePayExpressButton(),
    ) {
        confirmationHandler.awaitResultTurbine.add(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Payment failed"),
                message = "Payment failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        performer.confirm(expressButton)

        confirmationHandler.startTurbine.awaitItem()
        val call = eventReporter.calls.awaitItem()
        assertThat(call).isInstanceOf(FakeExpressCheckoutElementEventReporter.Call.OnEcePaymentFailure::class.java)
        val failureCall = call as FakeExpressCheckoutElementEventReporter.Call.OnEcePaymentFailure
        assertThat(failureCall.expressButton).isEqualTo(expressButton)
        assertThat(failureCall.error.cause.message).isEqualTo("Payment failed")
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error.message).isEqualTo("Payment failed")
    }

    @Test
    fun `confirm reports canceled when confirmation is canceled`() = runScenario(
        state = googlePayState(),
        expressButton = createGooglePayExpressButton(),
    ) {
        confirmationHandler.awaitResultTurbine.add(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.None)
        )

        performer.confirm(expressButton)

        confirmationHandler.startTurbine.awaitItem()
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
    }

    private fun googlePayState(): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .build(),
        )
    }

    private fun createGooglePayExpressButton(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ): ExpressButton.GooglePay {
        return ExpressButton.GooglePay.create(
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayConfiguration = GooglePayConfiguration(
                GooglePayConfiguration.Environment.Test,
            ).build(),
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        expressButton: ExpressButton,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeExpressCheckoutElementEventReporter()
        val errorReporter = FakeErrorReporter()
        val resultTurbine = Turbine<CheckoutController.Result>()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        stateHolder.state = state
        val performer = DefaultExpressCheckoutElementConfirmationPerformer(
            stateHolder = stateHolder,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            resultCallback = CheckoutController.ResultCallback(resultTurbine::add),
            statusBarColor = null,
            viewModelScope = backgroundScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            stateHolder = stateHolder,
            expressButton = expressButton,
            resultTurbine = resultTurbine,
        ).block()

        confirmationHandler.validate()
        eventReporter.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        resultTurbine.ensureAllEventsConsumed()
    }

    private class Scenario(
        val performer: DefaultExpressCheckoutElementConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeExpressCheckoutElementEventReporter,
        val errorReporter: FakeErrorReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val expressButton: ExpressButton,
        val resultTurbine: Turbine<CheckoutController.Result>,
    )
}
