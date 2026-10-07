package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedPaymentElementInitializerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val callbackTestRule = PaymentElementCallbackTestRule()

    @Test
    fun `initialize init and clear sheetLauncher`() = testScenario {
        assertThat(sheetStateHolder.sheetLauncher).isNull()
        initializer.initialize(applicationIsTaskOwner = true, removeCallbacksOnDestroy = true)
        assertThat(sheetStateHolder.sheetLauncher).isNotNull()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(sheetStateHolder.sheetLauncher).isNull()
    }

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = testScenario {
        initializer.initialize(applicationIsTaskOwner = false, removeCallbacksOnDestroy = true)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(applicationIsTaskOwner = false, removeCallbacksOnDestroy = true)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `when lifecycle is destroyed, should un-initialize callbacks`() {
        val owner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        val callbacks = PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("Not implemented")
            }
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("Not implemented")
            }
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("Not implemented")
            }
            .build()

        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER] = callbacks

        testScenario(owner, PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER) {
            initializer.initialize(applicationIsTaskOwner = true, removeCallbacksOnDestroy = true)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isEqualTo(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isEqualTo(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isEqualTo(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isNull()
        }
    }

    private fun testScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        paymentElementCallbackIdentifier: String = PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val sheetStateHolder = SheetStateHolder(SavedStateHandle())
        val eventReporter = FakeEventReporter()
        val initializer = EmbeddedPaymentElementInitializer(
            sheetLauncher = FakeEmbeddedSheetLauncher(),
            sheetStateHolder = sheetStateHolder,
            lifecycleOwner = lifecycleOwner,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
        )
        Scenario(
            initializer = initializer,
            sheetStateHolder = sheetStateHolder,
            lifecycleOwner = lifecycleOwner,
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val initializer: EmbeddedPaymentElementInitializer,
        val sheetStateHolder: SheetStateHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val eventReporter: FakeEventReporter,
    )

    private companion object {
        private const val PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "EmbeddedPaymentElementTestIdentifier"
    }
}
