package com.stripe.android.paymentelement.confirmation.intent

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(TestParameterInjector::class)
internal class CheckoutSessionConfirmationFinalizerTest {
    @Test
    fun `merges only fields implied by the client result`(
        @TestParameter intentCase: IntentCase,
        @TestParameter completedPoll: Boolean,
    ) = runScenario(
        outcome = if (completedPoll) {
            CheckoutSessionPoller.Outcome.COMPLETED
        } else {
            CheckoutSessionPoller.Outcome.TIMED_OUT
        },
    ) {
        val intent = intentCase.intent()
        val action = finalizer.finalize(response, intent) as ConfirmationDefinition.Action.Complete
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(action.intent).isSameInstanceAs(intent)
        val merged = requireNotNull(action.metadata[CheckoutSessionResponseKey])
        val expectedStatus = if (intentCase.complete || (intent == null && completedPoll)) {
            CheckoutSessionResponse.Status.COMPLETE
        } else {
            response.status
        }
        val expectedPaymentStatus = if (intentCase == IntentCase.PAYMENT_SUCCEEDED) {
            CheckoutSessionResponse.PaymentStatus.PAID
        } else {
            response.paymentStatus
        }
        assertThat(merged.status).isEqualTo(expectedStatus)
        assertThat(merged.paymentStatus).isEqualTo(expectedPaymentStatus)
        assertThat(merged.paymentIntent).isEqualTo(intent as? PaymentIntent)
        assertThat(merged.setupIntent).isEqualTo(intent as? SetupIntent)
        assertThat(merged.elementsSession?.stripeIntent).isEqualTo(intent ?: response.elementsSession?.stripeIntent)
        assertThat(
            merged.copy(
                status = response.status,
                paymentStatus = response.paymentStatus,
                paymentIntent = response.paymentIntent,
                setupIntent = response.setupIntent,
                elementsSession = response.elementsSession,
            )
        ).isEqualTo(response)
    }

    @Test
    fun `complete sessions skip polling`() = runScenario {
        val complete = response.copy(status = CheckoutSessionResponse.Status.COMPLETE)
        val action = finalizer.finalize(complete, null) as ConfirmationDefinition.Action.Complete
        assertThat(action.metadata[CheckoutSessionResponseKey]).isEqualTo(complete)
    }

    @Test
    fun `expired confirmation responses fail without polling`() = runScenario {
        val expired = response.copy(status = CheckoutSessionResponse.Status.EXPIRED)
        val action = finalizer.finalize(expired, null) as ConfirmationDefinition.Action.Fail
        assertThat(action.metadata[CheckoutSessionResponseKey]).isEqualTo(expired)
        assertThat(action.cause.message).contains("expired after confirmation")
    }

    @Test
    fun `payment failure outcomes retrieve and publish the latest session`(
        @TestParameter(value = ["REQUIRES_PAYMENT_METHOD", "FAILED_ASYNC_PAYMENT"])
        outcome: CheckoutSessionPoller.Outcome,
    ) = runScenario(outcome = outcome) {
        val latest = response.copy(customerEmail = "updated@example.com")
        retrieveResult = Result.success(latest)
        val action = finalizer.finalize(response, PaymentIntentFactory.create()) as ConfirmationDefinition.Action.Fail
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(retrieveCalls.awaitItem()).isEqualTo(response.id)
        assertThat(action.metadata[CheckoutSessionResponseKey]).isSameInstanceAs(latest)
        assertThat(action.cause.message).isEqualTo("Generic payment error")
    }

    @Test
    fun `invalid sessions retrieve the latest response and fail with an unexpected error`() = runScenario(
        outcome = CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED,
    ) {
        val latest = response.copy(status = CheckoutSessionResponse.Status.EXPIRED)
        retrieveResult = Result.success(latest)
        val action = finalizer.finalize(response, null) as ConfirmationDefinition.Action.Fail
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(retrieveCalls.awaitItem()).isEqualTo(response.id)
        assertThat(action.metadata[CheckoutSessionResponseKey]).isEqualTo(latest)
        assertThat(action.cause.message).contains("invalid or expired during polling")
    }

    @Test
    fun `retrieval failure retains the original response`(
        @TestParameter(value = ["REQUIRES_PAYMENT_METHOD", "FAILED_ASYNC_PAYMENT", "INVALID_OR_EXPIRED"])
        outcome: CheckoutSessionPoller.Outcome,
    ) = runScenario(outcome = outcome) {
        val error = IllegalStateException("Retrieval failed")
        retrieveResult = Result.failure(error)
        val action = finalizer.finalize(response, null) as ConfirmationDefinition.Action.Fail
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(retrieveCalls.awaitItem()).isEqualTo(response.id)
        assertThat(action.metadata[CheckoutSessionResponseKey]).isSameInstanceAs(response)
        assertThat(action.cause).isSameInstanceAs(error)
    }

    @Test
    fun `retrieval cancellation propagates`() = runScenario(
        outcome = CheckoutSessionPoller.Outcome.FAILED_ASYNC_PAYMENT,
    ) {
        retrieveResult = Result.failure(CancellationException("Canceled"))
        assertFailsWith<CancellationException> { finalizer.finalize(response, null) }
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(retrieveCalls.awaitItem()).isEqualTo(response.id)
    }

    private fun runScenario(
        outcome: CheckoutSessionPoller.Outcome = CheckoutSessionPoller.Outcome.COMPLETED,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val scenario = Scenario(outcome)
        scenario.block()
        scenario.pollCalls.ensureAllEventsConsumed()
        scenario.retrieveCalls.ensureAllEventsConsumed()
    }

    private class Scenario(outcome: CheckoutSessionPoller.Outcome) {
        val pollCalls = Turbine<String>()
        val retrieveCalls = Turbine<String>()
        val response = CheckoutSessionResponseFactory.create(
            paymentStatus = CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED,
            elementsSession = ElementsSession.createFromFallback(SetupIntentFactory.createDeferredIntent(), null),
        )
        var retrieveResult = Result.success(response)
        val finalizer = CheckoutSessionConfirmationFinalizer(
            poller = CheckoutSessionPoller {
                pollCalls.add(it)
                outcome
            },
            retrieveSession = {
                retrieveCalls.add(it)
                retrieveResult
            },
            genericErrorMessage = "Generic payment error",
        )
    }

    enum class IntentCase(val complete: Boolean) {
        PAYMENT_SUCCEEDED(true), PAYMENT_PROCESSING(true), PAYMENT_REQUIRES_CAPTURE(false),
        SETUP_SUCCEEDED(true), SETUP_PROCESSING(false), SETUP_REQUIRES_ACTION(false), NONE(false);

        fun intent(): StripeIntent? = when (this) {
            PAYMENT_SUCCEEDED -> PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            PAYMENT_PROCESSING -> PaymentIntentFactory.create(status = StripeIntent.Status.Processing)
            PAYMENT_REQUIRES_CAPTURE -> PaymentIntentFactory.create(status = StripeIntent.Status.RequiresCapture)
            SETUP_SUCCEEDED -> SetupIntentFactory.create(status = StripeIntent.Status.Succeeded)
            SETUP_PROCESSING -> SetupIntentFactory.create(status = StripeIntent.Status.Processing)
            SETUP_REQUIRES_ACTION -> SetupIntentFactory.create(status = StripeIntent.Status.RequiresAction)
            NONE -> null
        }
    }
}
