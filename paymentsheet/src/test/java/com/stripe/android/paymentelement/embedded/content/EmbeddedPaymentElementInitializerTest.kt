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
    val callbackRule = PaymentElementCallbackTestRule()

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = runScenario {
        initializer.initialize(applicationIsTaskOwner = false, retainCallbacks = false)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(applicationIsTaskOwner = false, retainCallbacks = false)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `unnamed lifecycle removes callbacks and releases the sheet launcher on destruction`() {
        val owner = TestLifecycleOwner()
        val callbacks = PaymentElementCallbacks.Builder().build()
        val unrelatedCallbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences["unrelated-owner"] = unrelatedCallbacks
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER] = callbacks

        runScenario(owner, PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER) {
            assertThat(sheetStateHolder.sheetLauncher).isNull()
            initializer.initialize(applicationIsTaskOwner = true, retainCallbacks = false)
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
    fun `named lifecycle retains callbacks and releases the sheet launcher on destruction`() = runScenario {
        val callbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences["unrelated-owner"] = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER] = callbacks
        assertThat(sheetStateHolder.sheetLauncher).isNull()
        initializer.initialize(applicationIsTaskOwner = true, retainCallbacks = true)
        assertThat(sheetStateHolder.sheetLauncher).isNotNull()
        assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
            .isSameInstanceAs(callbacks)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
            .isSameInstanceAs(callbacks)
        assertThat(sheetStateHolder.sheetLauncher).isNull()
    }

    private fun runScenario(
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
