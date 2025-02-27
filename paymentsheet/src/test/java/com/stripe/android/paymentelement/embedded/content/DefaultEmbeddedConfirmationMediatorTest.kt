package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfirmationMediatorTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `confirm should return false when the state is null`() = testScenario(
        loadedState = null,
    ) {
        assertThat(confirmationMediator.confirm()).isFalse()
    }

    @Test
    fun `confirm should return false when the selection is null`() = testScenario(
        loadedState = defaultLoadedState().copy(selection = null)
    ) {
        assertThat(confirmationMediator.confirm()).isFalse()
    }

    @Test
    fun `confirm should call the handler when state & selection are available`() = testScenario {
        assertThat(confirmationMediator.confirm()).isTrue()

        val args = confirmationHandler.startTurbine.awaitItem()

        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
    }

    @Test
    fun `successful confirm clears confirmation state and selection`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        confirmationMediator.result.test {
            assertThat(awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()

            assertThat(confirmationStateHolder.state).isNull()
            assertThat(selectionHolder.selection.value).isNull()
        }
    }

    @Test
    fun `failed confirm should return failed result with confirm exception`() = testScenario {
        val exception = IllegalStateException("Test failure.")

        confirmationMediator.result.test {
            confirmationHandler.state.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = exception,
                    message = "Error".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                )
            )

            assertThat(awaitItem()).isEqualTo(EmbeddedPaymentElement.Result.Failed(exception))
        }
    }

    @Test
    fun `cancelled confirm does not clear confirmation state and selection`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )

        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(selectionHolder.selection.value).isNotNull()

        confirmationMediator.result.test {
            assertThat(awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
        }
    }

    @Test
    fun `result should be able to be collected once`() = testScenario {
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        confirmationMediator.result.test {
            assertThat(awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
        }

        confirmationMediator.result.test {
            expectNoEvents()
        }
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
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()

        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle).apply {
            set(loadedState?.selection)
        }

        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        ).apply {
            state = loadedState
        }

        val confirmationMediator = DefaultEmbeddedConfirmationMediator(
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
            coroutineScope = backgroundScope,
        )

        Scenario(
            confirmationMediator = confirmationMediator,
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
        ).block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationMediator: EmbeddedConfirmationMediator,
        val confirmationHandler: FakeConfirmationHandler,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
