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
    val callbackTestRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize init and clear sheetLauncher`() = runScenario {
        assertThat(sheetStateHolder.sheetLauncher).isNull()
        initializer.initialize(true)
        assertThat(sheetStateHolder.sheetLauncher).isNotNull()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(sheetStateHolder.sheetLauncher).isNull()
    }

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = runScenario {
        initializer.initialize(false)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(false)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `Destroying an initialized element does not remove callbacks registered by another owner`() = runScenario {
        val callbackOwner = TestLifecycleOwner()
        val callbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences.register(PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER, callbackOwner, callbacks)
        initializer.initialize(true)
        assertThat(sheetStateHolder.sheetLauncher).isNotNull()

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER])
            .isSameInstanceAs(callbacks)
        assertThat(sheetStateHolder.sheetLauncher).isNull()
        callbackOwner.currentState = Lifecycle.State.DESTROYED
    }

    private fun runScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
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
