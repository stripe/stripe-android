package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse.ConfirmationStatus
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class DefaultCheckoutConfirmationFinalizerTest {

    @Test
    fun `complete session finishes without polling`() = runScenario {
        val response = confirmResponse.copy(status = CheckoutSessionResponse.Status.COMPLETE)

        val result = finalizer.finalize(response)

        assertThat(result.getOrThrow()).isEqualTo(response)
    }

    @Test
    fun `successful poll applies the polled session state`() = runScenario {
        api.pollResults.add {
            Result.success(
                pollResponse(
                    confirmationStatus = ConfirmationStatus.SUCCEEDED,
                    status = CheckoutSessionResponse.Status.COMPLETE,
                    paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                )
            )
        }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        assertThat(result.getOrThrow()).isEqualTo(
            confirmResponse.copy(
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )
        assertThat(elapsed()).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `pending and failed requests are polled again after the interval`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.PENDING)) }
        api.pollResults.add { Result.failure(APIConnectionException()) }
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.SUCCEEDED)) }

        val result = finalizer.finalize(confirmResponse)

        repeat(3) {
            assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        }
        assertThat(result.isSuccess).isTrue()
        assertThat(elapsed()).isEqualTo(1000.milliseconds)
    }

    @Test
    fun `timeout without any poll response keeps the confirm response`() = runScenario {
        api.pollResults.add { awaitCancellation() }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        assertThat(result.getOrThrow()).isEqualTo(confirmResponse)
        assertThat(elapsed()).isEqualTo(30.seconds)
    }

    @Test
    fun `timeout applies the last poll response`() = runScenario {
        api.pollResults.add {
            Result.success(
                pollResponse(
                    confirmationStatus = ConfirmationStatus.PENDING,
                    status = CheckoutSessionResponse.Status.COMPLETE,
                    paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                )
            )
        }
        api.pollResults.add { awaitCancellation() }

        val result = finalizer.finalize(confirmResponse)

        repeat(2) {
            assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        }
        assertThat(result.getOrThrow()).isEqualTo(
            confirmResponse.copy(
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )
        assertThat(elapsed()).isEqualTo(30.seconds)
    }

    @Test
    fun `failed confirmation fails`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.FAILED)) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Checkout Session confirmation failed.")
    }

    @Test
    fun `not started fails and reports an unexpected error`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.NOT_STARTED)) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.pollCalls.awaitItem()).isEqualTo(PollCall(SESSION_ID, clientSecret))
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_CONFIRMATION_NOT_STARTED)
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo(
            "Checkout Session confirmation unexpectedly reported not_started after an accepted /confirm."
        )
    }

    private fun pollResponse(
        confirmationStatus: ConfirmationStatus,
        status: CheckoutSessionResponse.Status = CheckoutSessionResponse.Status.OPEN,
        paymentStatus: CheckoutSessionResponse.PaymentStatus = CheckoutSessionResponse.PaymentStatus.UNPAID,
    ) = CheckoutClientPollResponse(
        id = SESSION_ID,
        confirmationStatus = confirmationStatus,
        status = status,
        paymentStatus = paymentStatus,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val api = FakePollApi()
        val errorReporter = FakeErrorReporter()
        val start = testScheduler.timeSource.markNow()
        val finalizer = DefaultCheckoutConfirmationFinalizer(
            poll = api::poll,
            errorReporter = errorReporter,
        )

        Scenario(
            finalizer = finalizer,
            api = api,
            errorReporter = errorReporter,
            elapsed = { start.elapsedNow() },
        ).block()

        api.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val finalizer: DefaultCheckoutConfirmationFinalizer,
        val api: FakePollApi,
        val errorReporter: FakeErrorReporter,
        val elapsed: () -> Duration,
    ) {
        val confirmResponse = CheckoutSessionResponseFactory.create(
            id = SESSION_ID,
            clientSecret = CLIENT_SECRET,
        )
    }

    private class FakePollApi {
        val pollCalls = Turbine<PollCall>()
        val pollResults = ArrayDeque<suspend () -> Result<CheckoutClientPollResponse>>()

        suspend fun poll(
            sessionId: String,
            clientSecret: CheckoutSessionClientSecret,
        ): Result<CheckoutClientPollResponse> {
            pollCalls.add(PollCall(sessionId, clientSecret))
            return pollResults.removeFirst().invoke()
        }

        fun ensureAllEventsConsumed() {
            pollCalls.ensureAllEventsConsumed()
            assertThat(pollResults).isEmpty()
        }
    }

    private data class PollCall(val sessionId: String, val clientSecret: CheckoutSessionClientSecret)

    private val clientSecret = CheckoutSessionClientSecret(CLIENT_SECRET)

    private companion object {
        const val SESSION_ID = "cs_test_finalize"
        const val CLIENT_SECRET = "cs_test_finalize_secret_test"
    }
}
