package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
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
        state = googlePayState(paymentSelection = null),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when the selection cannot be converted to a confirmation option`() = runScenario(
        // Google Pay is selected but the configuration has no Google Pay set, so toConfirmationOption
        // returns null and there is nothing to confirm.
        state = CheckoutControllerStateFactory.create(
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
        assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        assertThat(args.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `confirm starts confirmation with a Link option`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Link(brand = LinkBrand.Link),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        ),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    @Test
    fun `confirm reports payment success when confirmation succeeds`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        confirmationHandler.awaitResultTurbine.add(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        performer.confirm()

        confirmationHandler.startTurbine.awaitItem()
        val call = eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(call.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(call.intentId).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED.id)
    }

    @Test
    fun `confirm reports payment failure when confirmation fails`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        confirmationHandler.awaitResultTurbine.add(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Payment failed"),
                message = "Payment failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        performer.confirm()

        confirmationHandler.startTurbine.awaitItem()
        val call = eventReporter.paymentFailureCalls.awaitItem()
        assertThat(call.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(call.error).isInstanceOf<PaymentSheetConfirmationError.Stripe>()
    }

    private fun googlePayState(
        paymentSelection: PaymentSelection?,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = paymentSelection,
            configuration = CheckoutController.Configuration()
                .googlePayConfiguration(GooglePayConfiguration(GooglePayConfiguration.Environment.Test))
                .build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        statusBarColor: Int? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.state = state
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            sessionRefresher = sessionRefresher,
            logger = Logger.noop(),
            resultCallback = {},
        )
        val performer = CheckoutConfirmationPerformer(
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
            operationCoordinator = operationCoordinator,
            eventReporter = eventReporter,
            statusBarColor = statusBarColor,
            viewModelScope = backgroundScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            stateHolder = stateHolder,
        ).block()

        confirmationHandler.validate()
        eventReporter.validate()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private class Scenario(
        val performer: CheckoutConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeEventReporter,
        val stateHolder: CheckoutControllerStateHolder,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 0x00FF00
    }
}
