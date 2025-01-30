package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfirmationHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun constructorWiresUpConfirmationHandlerToResultCallback() = testScenario {
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
    fun `successful confirm clears confirmationState and selection`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
        assertThat(confirmationStateHolder.state).isNull()
        assertThat(selectionHolder.selection.value).isNull()
    }

    @Test
    fun `cancelled confirm does not clear confirmationState and selection`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenLoadedStateIsNull() = testScenario(
        loadedState = null,
    ) {
        confirmationHelper.confirm()
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenNoSelection() = testScenario(
        loadedState = defaultLoadedState().copy(selection = null),
    ) {
        confirmationHelper.confirm()
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
    }

    @Test
    fun confirmCallsConfirmationHandlerStart() = testScenario {
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
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        selectionHolder.set(loadedState?.selection)
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        confirmationStateHolder.state = loadedState
        val confirmationHelper = DefaultEmbeddedConfirmationHelper(
            confirmationHandler = confirmationHandler,
            resultCallback = {
                resultCallbackTurbine.add(it)
            },
            activityResultCaller = mock(),
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
        )
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            resultCallbackTurbine = resultCallbackTurbine,
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
        ).block()
        resultCallbackTurbine.ensureAllEventsConsumed()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHelper: EmbeddedConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val resultCallbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result>,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
