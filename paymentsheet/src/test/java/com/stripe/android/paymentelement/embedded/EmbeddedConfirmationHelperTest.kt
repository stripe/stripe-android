package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class EmbeddedConfirmationHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun constructorRegistersWithConfirmationHandler() = testScenario {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun constructorWiresUpConfirmationHandlerToResultCallback() = testScenario {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()

        val exception = IllegalStateException("Test failure.")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = exception,
                message = "Error".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        )
        assertThat(resultCallbackTurbine.awaitItem()).isEqualTo(
            EmbeddedPaymentElement.Result.Failed(exception)
        )
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenLoadedStateIsNull() = testScenario(
        loadedState = null,
    ) {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        confirmationHelper.confirm()
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenNoSelection() = testScenario(
        loadedState = defaultLoadedState().copy(selection = null),
    ) {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        confirmationHelper.confirm()
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
    }

    @Test
    fun confirmCallsConfirmationHandlerStart() = testScenario {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        confirmationHelper.confirm()
        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
    }

    @Test
    fun confirmCallsConfirmationHandlerStartWithLink() = testScenario(
        loadedState = defaultLoadedState().copy(
            selection = PaymentSelection.Link,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                )
            )
        )
    ) {
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        confirmationHelper.confirm()
        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    private fun defaultLoadedState(): EmbeddedConfirmationStateHolder.State {
        return EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = PaymentSelection.GooglePay,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000,
                        currency = "USD",
                    ),
                ),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .build()
        )
    }

    private fun testScenario(
        loadedState: EmbeddedConfirmationStateHolder.State? = defaultLoadedState(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val resultCallbackTurbine = Turbine<EmbeddedPaymentElement.Result>()
        val confirmationHandler = FakeConfirmationHandler()
        val confirmationHelper = EmbeddedConfirmationHelper(
            confirmationHandler = confirmationHandler,
            resultCallback = {
                resultCallbackTurbine.add(it)
            },
            activityResultCaller = mock(),
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
            confirmationStateSupplier = { loadedState }
        )
        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            resultCallbackTurbine = resultCallbackTurbine,
        ).block()
        resultCallbackTurbine.ensureAllEventsConsumed()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHelper: EmbeddedConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val resultCallbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result>,
    )
}
