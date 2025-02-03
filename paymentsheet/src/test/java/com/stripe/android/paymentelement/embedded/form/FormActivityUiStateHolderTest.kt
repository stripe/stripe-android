package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.ui.core.R
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
class FormActivityUiStateHolderTest {
    @Test
    fun `state initializes correctly`() = testScenario {
        stateHolder.state.test {
            val state = awaitItem()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
            assertThat(state.isEnabled).isFalse()
            assertThat(state.primaryButtonLabel).isEqualTo(
                resolvableString(
                    id = R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("$10.99")
                )
            )
        }
    }

    @Test
    fun `state returns label from config if provided`() {
        testScenario(
            config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .primaryButtonLabel("Test Label")
                .build()
        ) {
            stateHolder.state.test {
                assertThat(awaitItem().primaryButtonLabel).isEqualTo("Test Label".resolvableString)
            }
        }
    }

    @Test
    fun `state returns correct label for setup intent`() {
        testScenario(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD) {
            stateHolder.state.test {
                assertThat(awaitItem().primaryButtonLabel).isEqualTo(
                    R.string.stripe_setup_button_label.resolvableString
                )
            }
        }
    }

    @Test
    fun `state updates isEnabled when selection is set`() = testScenario {
        stateHolder.state.test {
            assertThat(awaitItem().isEnabled).isFalse()
            selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem().isEnabled).isTrue()
        }
    }

    @Test
    fun `state updates processing correctly while confirming`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.set(selection)
        stateHolder.updateProcessingState(confirmationStateConfirming(selection))
        stateHolder.state.test {
            val state = awaitItem()
            assertThat(state.isEnabled).isFalse()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Processing)
            assertThat(state.isProcessing).isTrue()
        }
    }

    @Test
    fun `state updates when confirmation is successful`() = testScenario {
        stateHolder.updateProcessingState(confirmationStateComplete(true))
        stateHolder.state.test {
            val state = awaitItem()
            assertThat(state.processingState).isEqualTo(PrimaryButtonProcessingState.Completed)
            assertThat(state.isProcessing).isFalse()
        }
    }

    @Test
    fun `state re-enables if confirmation fails`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.set(selection)
        stateHolder.updateProcessingState(confirmationStateConfirming(selection))
        stateHolder.state.test {
            val processingState = awaitItem()
            assertThat(processingState.isProcessing).isTrue()
            assertThat(processingState.isEnabled).isFalse()
        }

        stateHolder.updateProcessingState(confirmationStateComplete(false))

        stateHolder.state.test {
            val failedState = awaitItem()
            assertThat(failedState.isEnabled).isTrue()
            assertThat(failedState.isProcessing).isFalse()
            assertThat(failedState.processingState).isEqualTo(PrimaryButtonProcessingState.Idle(null))
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val stateHolder: FormActivityUiStateHolder
    )

    private fun testScenario(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        config: EmbeddedPaymentElement.Configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(stripeIntent = stripeIntent)
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val stateHolder = FormActivityUiStateHolder(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = config
        )

        Scenario(
            selectionHolder = selectionHolder,
            stateHolder = stateHolder
        ).block()
    }

    private fun confirmationStateConfirming(selection: PaymentSelection): ConfirmationHandler.State.Confirming {
        val confirmationOption = selection.toConfirmationOption(
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration.asCommonConfiguration(),
            linkConfiguration = null
        )
        return ConfirmationHandler.State.Confirming(requireNotNull(confirmationOption))
    }

    private fun confirmationStateComplete(succeeded: Boolean): ConfirmationHandler.State.Complete {
        val result = if (succeeded) {
            ConfirmationHandler.Result.Succeeded(
                PaymentIntentFixtures.PI_SUCCEEDED,
                null
            )
        } else {
            ConfirmationHandler.Result.Failed(
                cause = Throwable(),
                message = "Whoops".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal
            )
        }
        return ConfirmationHandler.State.Complete(result)
    }
}
