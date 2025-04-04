package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedPaymentElementInitializerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize init and clear sheetLauncher`() = testScenario {
        assertThat(contentHelper.testSheetLauncher).isNull()
        initializer.initialize(true)
        assertThat(contentHelper.testSheetLauncher).isNotNull()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(contentHelper.testSheetLauncher).isNull()
    }

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = testScenario {
        initializer.initialize(false)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(false)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    @Test
    @OptIn(ExperimentalCustomPaymentMethodsApi::class)
    fun `when lifecycle is destroyed, should un-initialize callbacks`() {
        val owner = TestLifecycleOwner()
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
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

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
                .isNotNull()
        }
    }

    private fun testScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        paymentElementCallbackIdentifier: String = PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val eventReporter = FakeEventReporter()
        val initializer = EmbeddedPaymentElementInitializer(
            sheetLauncher = FakeEmbeddedSheetLauncher(),
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
            savedStateHandle = MainThreadSavedStateHandle(SavedStateHandle()),
            eventReporter = eventReporter,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
        )
        Scenario(
            initializer = initializer,
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val initializer: EmbeddedPaymentElementInitializer,
        val contentHelper: FakeEmbeddedContentHelper,
        val lifecycleOwner: TestLifecycleOwner,
        val eventReporter: FakeEventReporter,
    )

    private companion object {
        private const val PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "EmbeddedPaymentElementTestIdentifier"
    }
}
