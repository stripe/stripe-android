package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutEventReporterTestFactory
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `success is reported once after restoration and clears saved context`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        assertThat(savedConfirmationKeys()).hasSize(2)
        assertThat(events()).isEmpty()

        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)

        val success = successfulResult()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        runCurrent()

        val terminalEvents = events()
        assertThat(terminalEvents.map { it["event"] })
            .containsExactly("checkout_elements.confirm.succeeded")
        assertThat(terminalEvents.single()).containsEntry("selected_lpm", "google_pay")
        assertThat(terminalEvents.single()).containsEntry("duration", 2f)
        assertThat(terminalEvents.single()).containsEntry(
            "intent_id",
            PaymentIntentFixtures.PI_SUCCEEDED.id,
        )
        assertThat(terminalEvents.single()).containsEntry("deferred_intent_confirmation_type", "client")
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
    }

    @Test
    fun `payment failure reports safe fields`() = runScenario {
        startReporting()
        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.GooglePay)
        val privateMessage = "private payment details"
        val error = APIException(
            stripeError = StripeError(
                type = "card_error",
                code = "card_declined",
                message = privateMessage,
            ),
            requestId = "req_failed_confirmation",
            message = privateMessage,
        )
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = error,
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
        runCurrent()

        val terminal = events().single()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(terminal).containsEntry("error_code", "card_declined")
        assertThat(terminal).containsEntry("duration", 2f)
        assertThat(terminal.toString()).doesNotContain(privateMessage)
        assertThat(savedConfirmationKeys()).isEmpty()
    }

    @Test
    fun `canceled confirmation does not report a terminal event`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
        runCurrent()

        assertThat(events()).isEmpty()
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isFalse()
    }

    @Test
    fun `internal failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.Internal
    )

    @Test
    fun `merchant integration failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration
    )

    @Test
    fun `fatal failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal
    )

    private fun assertFilteredFailure(
        type: ConfirmationHandler.Result.Failed.ErrorType,
    ) = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("filtered failure"),
                message = "Filtered failure".resolvableString,
                type = type,
            )
        )
        runCurrent()

        assertThat(events()).isEmpty()
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isFalse()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider(duration = 2.seconds)
        val reporter = CheckoutEventReporterTestFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            checkoutEventReporter = reporter,
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
        )

        scenario.block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val savedStateHandle: SavedStateHandle,
        private val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        private val checkoutEventReporter: CheckoutEventReporter,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        val performer: CheckoutAnalyticsPerformer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun startReporting(performer: CheckoutAnalyticsPerformer = this.performer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        fun runCurrent() {
            testScheduler.runCurrent()
        }

        fun events(): List<Map<String, *>> =
            analyticsRequestExecutor.getExecutedRequests().map { it.params }

        fun savedConfirmationKeys(): Set<String> =
            savedStateHandle.keys().filter { it.startsWith("CheckoutAnalyticsPerformer.") }.toSet()

        private fun createPerformer(): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            checkoutEventReporter = checkoutEventReporter,
            savedStateHandle = savedStateHandle,
        )
    }

    private companion object {
        fun successfulResult() = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Client)
            },
        )
    }
}
