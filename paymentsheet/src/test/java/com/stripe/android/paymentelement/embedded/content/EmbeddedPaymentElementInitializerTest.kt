package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.EmbeddedPaymentElement
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
    val callbackRule = PaymentElementCallbackTestRule()

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = runScenario {
        initializer.initialize(applicationIsTaskOwner = false)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(applicationIsTaskOwner = false)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `lifecycle removes callbacks and releases the sheet launcher on destruction`() {
        val owner = TestLifecycleOwner()
        val callbacks = PaymentElementCallbacks.Builder().build()
        val unrelatedCallbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences["unrelated-owner"] = unrelatedCallbacks
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER] = callbacks

        runScenario(owner, PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER) {
            assertThat(sheetStateHolder.sheetLauncher).isNull()
            initializer.initialize(applicationIsTaskOwner = true)
            assertThat(sheetStateHolder.sheetLauncher).isNotNull()
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isSameInstanceAs(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isSameInstanceAs(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isSameInstanceAs(callbacks)

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
                .isSameInstanceAs(unrelatedCallbacks)
            assertThat(sheetStateHolder.sheetLauncher).isNull()
        }
    }

    @Test
    fun `destroyed lifecycle clears its result callback binding`() = runScenario {
        val callback = EmbeddedPaymentElement.ResultCallback { error("Unexpected result") }
        stateHolder.lifecycleOwner = lifecycleOwner
        stateHolder.resultCallback = callback
        initializer.initialize(applicationIsTaskOwner = true)
        assertThat(stateHolder.lifecycleOwner).isSameInstanceAs(lifecycleOwner)
        assertThat(stateHolder.resultCallback).isSameInstanceAs(callback)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(stateHolder.lifecycleOwner).isNull()
        assertThat(stateHolder.resultCallback).isNull()
    }

    @Test
    fun `destroyed lifecycle does not clear another owners binding`() = runScenario {
        val otherOwner = TestLifecycleOwner()
        val callback = EmbeddedPaymentElement.ResultCallback { error("Unexpected result") }
        stateHolder.lifecycleOwner = otherOwner
        stateHolder.resultCallback = callback
        initializer.initialize(applicationIsTaskOwner = true)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(stateHolder.lifecycleOwner).isSameInstanceAs(otherOwner)
        assertThat(stateHolder.resultCallback).isSameInstanceAs(callback)
    }

    private fun runScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        paymentElementCallbackIdentifier: String = PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val sheetStateHolder = SheetStateHolder(SavedStateHandle())
        val stateHolder = EmbeddedPaymentElementStateHolder()
        val eventReporter = FakeEventReporter()
        val initializer = EmbeddedPaymentElementInitializer(
            sheetLauncher = FakeEmbeddedSheetLauncher(),
            sheetStateHolder = sheetStateHolder,
            lifecycleOwner = lifecycleOwner,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            stateHolder = stateHolder,
        )
        Scenario(
            initializer = initializer,
            sheetStateHolder = sheetStateHolder,
            lifecycleOwner = lifecycleOwner,
            eventReporter = eventReporter,
            stateHolder = stateHolder,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val initializer: EmbeddedPaymentElementInitializer,
        val sheetStateHolder: SheetStateHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val eventReporter: FakeEventReporter,
        val stateHolder: EmbeddedPaymentElementStateHolder,
    )

    private companion object {
        private const val PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "EmbeddedPaymentElementTestIdentifier"
    }
}
