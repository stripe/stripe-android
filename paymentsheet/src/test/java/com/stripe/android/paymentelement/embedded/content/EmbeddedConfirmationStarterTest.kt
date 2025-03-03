package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EmbeddedConfirmationStarterTest {
    @Test
    fun `on register, should call 'register' on confirmation handler`() = test {
        val activityResultCaller = DummyActivityResultCaller.noOp()
        val lifecycleOwner = TestLifecycleOwner(
            coroutineDispatcher = Dispatchers.Unconfined,
        )

        confirmationStarter.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        val registerCall = confirmationHandler.registerTurbine.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isEqualTo(lifecycleOwner)
    }

    @Test
    fun `on confirm, should call 'start' on confirmation handler`() = test {
        val arguments = ConfirmationHandler.Args(
            intent = PaymentIntentFactory.create(
                paymentMethod = PaymentMethodFactory.card(random = true),
            ),
            confirmationOption = FakeConfirmationOption(),
            appearance = PaymentSheet.Appearance(),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            shippingDetails = null,
        )

        confirmationStarter.start(arguments)

        val startCall = confirmationHandler.startTurbine.awaitItem()

        assertThat(startCall.confirmationOption).isEqualTo(arguments.confirmationOption)
        assertThat(startCall.intent).isEqualTo(arguments.intent)
        assertThat(startCall.appearance).isEqualTo(arguments.appearance)
        assertThat(startCall.initializationMode).isEqualTo(arguments.initializationMode)
        assertThat(startCall.shippingDetails).isEqualTo(arguments.shippingDetails)
    }

    @Test
    fun `on idle state, should not emit any result events`() = test(
        confirmationState = ConfirmationHandler.State.Idle,
    ) {
        confirmationStarter.result.test {
            expectNoEvents()
        }
    }

    @Test
    fun `on confirming state, should not emit any result events`() = test(
        confirmationState = ConfirmationHandler.State.Confirming(
            option = FakeConfirmationOption(),
        ),
    ) {
        confirmationStarter.result.test {
            expectNoEvents()
        }
    }

    @Test
    fun `on complete state, should emit result at most once`() {
        val intent = PaymentIntentFactory.create(
            paymentMethod = PaymentMethodFactory.card(random = true),
            status = StripeIntent.Status.Succeeded,
        )

        test(
            confirmationState = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Succeeded(
                    intent = intent,
                    deferredIntentConfirmationType = null,
                ),
            ),
        ) {
            confirmationStarter.result.test {
                val result = awaitItem().assertSucceeded()

                assertThat(result.intent).isEqualTo(intent)
                assertThat(result.deferredIntentConfirmationType).isNull()
            }

            confirmationStarter.result.test {
                expectNoEvents()
            }

            confirmationStarter.result.test {
                expectNoEvents()
            }
        }
    }

    private fun test(
        confirmationState: ConfirmationHandler.State = ConfirmationHandler.State.Idle,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler(
            state = MutableStateFlow(confirmationState)
        )

        Scenario(
            confirmationStarter = EmbeddedConfirmationStarter(
                confirmationHandler = confirmationHandler,
                coroutineScope = backgroundScope,
            ),
            confirmationHandler = confirmationHandler,
        ).block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationStarter: EmbeddedConfirmationStarter,
        val confirmationHandler: FakeConfirmationHandler,
    )
}
