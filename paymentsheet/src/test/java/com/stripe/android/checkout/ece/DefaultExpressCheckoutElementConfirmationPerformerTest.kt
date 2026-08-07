package com.stripe.android.checkout.ece

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
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
            val option = args.confirmationOption as GooglePayConfirmationOption
            assertThat(option.config.shippingAddressParameters).isNull()
            assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        }
    }

    @Test
    fun `confirm requests a Google Pay shipping address for allowed countries`() {
        val state = googlePayState(
            allowedShippingCountries = listOf("US", "CA"),
        )

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = state.paymentMethodMetadata,
                shippingAddressRequired = true,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
            val option = args.confirmationOption as GooglePayConfirmationOption
            assertThat(option.config.shippingAddressParameters).isEqualTo(
                GooglePayJsonFactory.ShippingAddressParameters(
                    isRequired = true,
                    allowedCountryCodes = setOf("US", "CA"),
                )
            )
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
    }

    @Test
    fun `confirm delivers failure when confirmation start throws`() {
        val expected = IllegalStateException("Start failed")
        runScenario(
            state = googlePayState(),
            expressButton = createGooglePayExpressButton(),
            startError = expected,
        ) {
            performer.confirm(expressButton)

            confirmationHandler.startTurbine.awaitItem()
            val result = resultTurbine.awaitItem()
            assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        }
    }

    @Test
    fun `confirm delivers failure when awaiting confirmation result throws`() {
        val expected = IllegalStateException("Await failed")
        runScenario(
            state = googlePayState(),
            expressButton = createGooglePayExpressButton(),
            awaitResultError = expected,
        ) {
            performer.confirm(expressButton)

            confirmationHandler.startTurbine.awaitItem()
            val result = resultTurbine.awaitItem()
            assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        }
    }

    @Test
    fun `ECE reporting failure is not delivered as a confirmation failure`() {
        val expected = IllegalStateException("Reporting failed")
        runScenario(
            state = googlePayState(),
            expressButton = createGooglePayExpressButton(),
            paymentSuccessError = expected,
        ) {
            confirmationHandler.awaitResultTurbine.add(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            performer.confirm(expressButton)

            confirmationHandler.startTurbine.awaitItem()
            assertThat(eventReporter.calls.awaitItem())
                .isEqualTo(FakeExpressCheckoutElementEventReporter.Call.OnEcePaymentSuccess(expressButton))
            assertThat(uncaughtErrors.awaitItem()).isSameInstanceAs(expected)
            resultTurbine.expectNoEvents()
        }
    }

    private fun googlePayState(
        allowedShippingCountries: List<String>? = null,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            configuration = CheckoutController.Configuration()
                .googlePayConfiguration(GooglePayConfiguration(GooglePayConfiguration.Environment.Test))
                .build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                merchantCountry = "US",
                allowedShippingCountries = allowedShippingCountries,
            ),
        )
    }

    private fun createGooglePayExpressButton(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        shippingAddressRequired: Boolean = false,
    ): ExpressButton.GooglePay {
        return ExpressButton.GooglePay.create(
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayConfiguration = GooglePayConfiguration(
                GooglePayConfiguration.Environment.Test,
            ).build(),
            shippingAddressRequired = shippingAddressRequired,
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        expressButton: ExpressButton,
        startError: Throwable? = null,
        awaitResultError: Throwable? = null,
        paymentSuccessError: Throwable? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler(
            startError = startError,
            awaitResultError = awaitResultError,
        )
        val eventReporter = FakeExpressCheckoutElementEventReporter(
            paymentSuccessError = paymentSuccessError,
        )
        val errorReporter = FakeErrorReporter()
        val resultTurbine = Turbine<CheckoutController.Result>()
        val uncaughtErrors = Turbine<Throwable>()
        val performerScope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, error ->
                uncaughtErrors.add(error)
            }
        )
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.state = state
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            resultCallback = CheckoutController.ResultCallback(resultTurbine::add),
            viewModelScope = backgroundScope,
        )
        val performer = DefaultExpressCheckoutElementConfirmationPerformer(
            stateHolder = stateHolder,
            confirmationHandler = confirmationHandler,
            operationCoordinator = operationCoordinator,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            statusBarColor = null,
            viewModelScope = performerScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            stateHolder = stateHolder,
            expressButton = expressButton,
            resultTurbine = resultTurbine,
            uncaughtErrors = uncaughtErrors,
        ).block()

        performerScope.cancel()
        confirmationHandler.validate()
        eventReporter.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        resultTurbine.ensureAllEventsConsumed()
        uncaughtErrors.ensureAllEventsConsumed()
    }

    private class Scenario(
        val performer: DefaultExpressCheckoutElementConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeExpressCheckoutElementEventReporter,
        val errorReporter: FakeErrorReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val expressButton: ExpressButton,
        val resultTurbine: Turbine<CheckoutController.Result>,
        val uncaughtErrors: Turbine<Throwable>,
    )
}
