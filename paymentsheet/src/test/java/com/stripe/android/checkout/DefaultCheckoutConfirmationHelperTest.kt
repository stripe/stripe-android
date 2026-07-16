package com.stripe.android.checkout

import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStarter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultCheckoutConfirmationHelperTest {

    @Test
    fun `confirm builds args from the state and starts the confirmation handler`() = testScenario {
        confirmationHelper.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
        assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        // Session mutations are blocked while confirmation is in flight.
        assertThat(stateHolder.state?.integrationLaunched).isTrue()
    }

    @Test
    fun `confirm builds a saved payment method option from the selection`() = testScenario(
        state = committedState(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ),
    ) {
        confirmationHelper.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()
        assertThat(stateHolder.state?.integrationLaunched).isTrue()
    }

    @Test
    fun `confirm reports Failed and does not start when there is no selection`() = testScenario(
        state = committedState(paymentSelection = null),
    ) {
        confirmationHelper.confirm()

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat(stateHolder.state?.integrationLaunched).isFalse()
    }

    @Test
    fun `confirm reports Failed and does not start when there is no committed state`() = testScenario(
        state = null,
    ) {
        confirmationHelper.confirm()

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Failed>()
    }

    @Test
    fun `succeeded confirmation maps to Completed`() = testScenario {
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(intent = PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `failed confirmation maps to Failed and re-enables mutations`() = testScenario(
        state = committedState(integrationLaunched = true),
    ) {
        val cause = IllegalStateException("Boom.")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = cause,
                message = "Boom.".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        )

        assertThat(results.awaitItem()).isEqualTo(CheckoutController.Result.Failed(cause))
        assertThat(stateHolder.state?.integrationLaunched).isFalse()
    }

    @Test
    fun `canceled confirmation maps to Canceled and re-enables mutations`() = testScenario(
        state = committedState(integrationLaunched = true),
    ) {
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
        assertThat(stateHolder.state?.integrationLaunched).isFalse()
    }

    private fun committedState(
        paymentSelection: PaymentSelection? = PaymentSelection.GooglePay,
        integrationLaunched: Boolean = false,
    ) = CheckoutControllerState(
        key = "test_key",
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(),
        integrationLaunched = integrationLaunched,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .googlePay(
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                )
            )
            .build(),
        paymentSelection = paymentSelection,
        temporarySelection = null,
        previousNewSelections = Bundle(),
    )

    private fun testScenario(
        state: CheckoutControllerState? = committedState(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val stateHolder = CheckoutControllerStateHolder(
            savedStateHandle = SavedStateHandle(),
            errorReporter = FakeErrorReporter(),
        )
        stateHolder.state = state
        val results = Turbine<CheckoutController.Result>()

        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val confirmationHelper = DefaultCheckoutConfirmationHelper(
            confirmationStarter = EmbeddedConfirmationStarter(
                confirmationHandler = confirmationHandler,
                coroutineScope = backgroundScope,
            ),
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            resultCallback = { results.add(it) },
            eventReporter = FakeEventReporter(),
        )
        // The handler is registered with the activity's caller and lifecycle when the helper is created.
        val registerCall = confirmationHandler.registerTurbine.awaitItem()
        assertThat(registerCall.activityResultCaller).isSameInstanceAs(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isSameInstanceAs(lifecycleOwner)

        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
            results = results,
        ).block()

        results.ensureAllEventsConsumed()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHelper: CheckoutConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val stateHolder: CheckoutControllerStateHolder,
        val results: Turbine<CheckoutController.Result>,
    )
}
