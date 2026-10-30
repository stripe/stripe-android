package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse.ConfirmationStatus
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CheckoutSessionPollerTest {

    @Test
    fun `pending continues until succeeded regardless of session status`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.PENDING, CheckoutSessionResponse.Status.COMPLETE)),
        Step.Success(pollResponse(ConfirmationStatus.SUCCEEDED)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.Succeeded(pollResponse(ConfirmationStatus.SUCCEEDED))
        )
        assertThat(awaitRequestStarts(2)).containsExactly(0.seconds, 0.5.seconds).inOrder()
    }

    @Test
    fun `failed stops even when session is complete`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.FAILED, CheckoutSessionResponse.Status.COMPLETE)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.Failed(
                pollResponse(ConfirmationStatus.FAILED, CheckoutSessionResponse.Status.COMPLETE)
            )
        )
        assertThat(awaitRequestStarts(1)).containsExactly(0.seconds)
    }

    @Test
    fun `not started stops with distinct outcome`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.NOT_STARTED)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.NotStarted(pollResponse(ConfirmationStatus.NOT_STARTED))
        )
        assertThat(awaitRequestStarts(1)).containsExactly(0.seconds)
    }

    @Test
    fun `timeout preserves latest response despite request failures`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.PENDING)),
        Step.Success(pollResponse(ConfirmationStatus.PENDING, CheckoutSessionResponse.Status.COMPLETE)),
        Step.Failure(APIConnectionException(), duration = 29.seconds),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.TimedOut(
                pollResponse(ConfirmationStatus.PENDING, CheckoutSessionResponse.Status.COMPLETE)
            )
        )
        assertThat(awaitRequestStarts(3)).containsExactly(0.seconds, 0.5.seconds, 1.seconds).inOrder()
    }

    @Test
    fun `rate limit too close to the deadline times out with last response`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.PENDING), duration = 28.seconds),
        Step.Failure(rateLimitError()),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.TimedOut(pollResponse(ConfirmationStatus.PENDING))
        )
        assertThat(awaitRequestStarts(2)).containsExactly(0.seconds, 28.seconds).inOrder()
        assertThat(elapsed).isEqualTo(28.seconds)
    }

    @Test
    fun `request that outlasts the deadline is cut off`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.PENDING)),
        Step.Hang,
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.TimedOut(pollResponse(ConfirmationStatus.PENDING))
        )
        assertThat(awaitRequestStarts(2)).containsExactly(0.seconds, 0.5.seconds).inOrder()
        assertThat(elapsed).isEqualTo(30.seconds)
    }

    @Test
    fun `request errors retry at the normal cadence`() = runScenario(
        Step.Failure(APIConnectionException()),
        Step.Success(pollResponse(ConfirmationStatus.SUCCEEDED)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.Succeeded(pollResponse(ConfirmationStatus.SUCCEEDED))
        )
        assertThat(awaitRequestStarts(2)).containsExactly(0.seconds, 0.5.seconds).inOrder()
    }

    @Test
    fun `requests are spaced start to start`() = runScenario(
        Step.Success(pollResponse(ConfirmationStatus.PENDING), duration = 100.milliseconds),
        Step.Success(pollResponse(ConfirmationStatus.PENDING), duration = 700.milliseconds),
        Step.Success(pollResponse(ConfirmationStatus.SUCCEEDED)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.Succeeded(pollResponse(ConfirmationStatus.SUCCEEDED))
        )
        assertThat(awaitRequestStarts(3))
            .containsExactly(0.seconds, 0.5.seconds, 1.2.seconds)
            .inOrder()
    }

    @Test
    fun `rate limits back off exponentially and stop at timeout`() = runScenario(
        *Array(5) { Step.Failure(rateLimitError()) },
    ) {
        assertThat(outcome).isEqualTo(CheckoutSessionPoller.Outcome.TimedOut(lastResponse = null))
        assertThat(awaitRequestStarts(5))
            .containsExactly(0.seconds, 2.seconds, 6.seconds, 14.seconds, 22.seconds)
            .inOrder()
    }

    @Test
    fun `only a successful poll resets rate limit backoff`() = runScenario(
        Step.Failure(rateLimitError()),
        Step.Failure(APIConnectionException()),
        Step.Success(pollResponse(ConfirmationStatus.PENDING)),
        Step.Failure(rateLimitError()),
        Step.Success(pollResponse(ConfirmationStatus.PENDING)),
        Step.Success(pollResponse(ConfirmationStatus.SUCCEEDED)),
    ) {
        assertThat(outcome).isEqualTo(
            CheckoutSessionPoller.Outcome.Succeeded(pollResponse(ConfirmationStatus.SUCCEEDED))
        )
        assertThat(awaitRequestStarts(6))
            .containsExactly(0.seconds, 2.seconds, 4.seconds, 4.5.seconds, 6.5.seconds, 7.seconds)
            .inOrder()
    }

    private fun rateLimitError() = APIException(statusCode = 429)

    private fun pollResponse(
        confirmationStatus: ConfirmationStatus,
        status: CheckoutSessionResponse.Status = CheckoutSessionResponse.Status.OPEN,
    ) = CheckoutClientPollResponse(
        id = "cs_test_123",
        confirmationStatus = confirmationStatus,
        status = status,
        paymentStatus = CheckoutSessionResponse.PaymentStatus.UNPAID,
    )

    private fun runScenario(
        vararg steps: Step,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val request = FakePollRequest(this, steps.toList())
        val start = testScheduler.timeSource.markNow()
        val outcome = CheckoutSessionPoller(testScheduler.timeSource).poll(request::invoke)

        Scenario(
            outcome = outcome,
            elapsed = start.elapsedNow(),
            requestStarts = request.requestStarts,
        ).block()

        request.requestStarts.ensureAllEventsConsumed()
    }

    private class Scenario(
        val outcome: CheckoutSessionPoller.Outcome,
        val elapsed: Duration,
        private val requestStarts: Turbine<Duration>,
    ) {
        suspend fun awaitRequestStarts(count: Int): List<Duration> = List(count) { requestStarts.awaitItem() }
    }

    private sealed interface Step {
        data class Success(
            val response: CheckoutClientPollResponse,
            val duration: Duration = Duration.ZERO,
        ) : Step

        data class Failure(
            val error: Throwable,
            val duration: Duration = Duration.ZERO,
        ) : Step

        data object Hang : Step
    }

    private class FakePollRequest(
        testScope: TestScope,
        steps: List<Step>,
    ) {
        private val remainingSteps = ArrayDeque(steps)
        private val start = testScope.testScheduler.timeSource.markNow()

        val requestStarts = Turbine<Duration>()

        suspend fun invoke(): Result<CheckoutClientPollResponse> {
            requestStarts.add(start.elapsedNow())
            return when (val step = remainingSteps.removeFirst()) {
                is Step.Success -> {
                    delay(step.duration)
                    Result.success(step.response)
                }
                is Step.Failure -> {
                    delay(step.duration)
                    Result.failure(step.error)
                }
                Step.Hang -> awaitCancellation()
            }
        }
    }
}
