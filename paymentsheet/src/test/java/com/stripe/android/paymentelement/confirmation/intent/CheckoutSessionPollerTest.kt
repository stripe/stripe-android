package com.stripe.android.paymentelement.confirmation.intent

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.exception.APIException
import com.stripe.android.paymentsheet.repositories.CheckoutSessionPollResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(TestParameterInjector::class)
internal class CheckoutSessionPollerTest {
    @Test
    fun `terminal states return the corresponding outcome`(@TestParameter state: TerminalState) = runScenario(
        responses = listOf(success(state.state)),
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(state.outcome)
        assertThat(request.calls.awaitItem()).isEqualTo(Call("cs_test", 0, 30_000))
    }

    @Test
    fun `in progress states continue polling`(@TestParameter state: InProgressState) = runScenario(
        responses = listOf(success(state.state), success(CheckoutSessionPollResponse.State.SUCCEEDED)),
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.COMPLETED)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
        assertThat(request.calls.awaitItem()).isEqualTo(Call("cs_test", 500, 29_500))
    }

    @Test
    fun `a payment requiring a payment method takes precedence over a completed poll state`() = runScenario(
        responses = listOf(
            Result.success(
                CheckoutSessionPollResponse(
                    sessionId = "cs_test",
                    state = CheckoutSessionPollResponse.State.SUCCEEDED,
                    paymentObjectStatus = "requires_payment_method",
                )
            )
        ),
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.REQUIRES_PAYMENT_METHOD)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
    }

    @Test
    fun `request and parsing errors are retried`() = runScenario(
        responses = listOf(
            Result.failure(IllegalStateException("Connection failed")),
            Result.failure(APIException(message = "Malformed response")),
            success(CheckoutSessionPollResponse.State.SUCCEEDED),
        ),
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.COMPLETED)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(500)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(1_000)
    }

    @Test
    fun `rate limits back off to eight seconds and success resets the interval`() = runScenario(
        responses = listOf(
            rateLimit(), rateLimit(), rateLimit(), rateLimit(),
            success(CheckoutSessionPollResponse.State.ACTIVE), rateLimit(),
            success(CheckoutSessionPollResponse.State.SUCCEEDED),
        ),
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.COMPLETED)
        val starts = List(7) { request.calls.awaitItem().startedAt }
        assertThat(starts).containsExactly(0L, 2_000L, 6_000L, 14_000L, 22_000L, 22_500L, 24_500L).inOrder()
    }

    @Test
    fun `slow requests start the next request immediately without an extra delay`() = runScenario(
        responses = listOf(
            success(CheckoutSessionPollResponse.State.ACTIVE),
            success(CheckoutSessionPollResponse.State.SUCCEEDED),
        ),
        requestDuration = 700,
    ) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.COMPLETED)
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
        assertThat(request.calls.awaitItem()).isEqualTo(Call("cs_test", 700, 29_300))
    }

    @Test
    fun `polling times out at thirty seconds without starting another request`() = runScenario {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.TIMED_OUT)
        val calls = List(60) { request.calls.awaitItem() }
        assertThat(calls.last()).isEqualTo(Call("cs_test", 29_500, 500))
        assertThat(scope.testScheduler.currentTime).isEqualTo(30_000)
    }

    @Test
    fun `an in flight request is bounded by the polling budget`() = runScenario(requestDuration = 40_000) {
        assertThat(poller.poll("cs_test")).isEqualTo(CheckoutSessionPoller.Outcome.TIMED_OUT)
        assertThat(request.calls.awaitItem().timeoutMillis).isEqualTo(30_000)
        assertThat(scope.testScheduler.currentTime).isEqualTo(30_000)
    }

    @Test
    fun `coroutine cancellation stops polling immediately`() = runScenario(requestDuration = 1_000) {
        val result = scope.async { poller.poll("cs_test") }
        scope.testScheduler.runCurrent()
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
        result.cancelAndJoin()
        assertThat(result.isCancelled).isTrue()
        assertThat(scope.testScheduler.currentTime).isEqualTo(0)
    }

    @Test
    fun `a cancellation returned as a request error is preserved`() = runScenario(
        responses = listOf(Result.failure(CancellationException("Canceled request"))),
    ) {
        assertFailsWith<CancellationException> { poller.poll("cs_test") }
        assertThat(request.calls.awaitItem().startedAt).isEqualTo(0)
    }

    private fun runScenario(
        responses: List<Result<CheckoutSessionPollResponse>> = emptyList(),
        requestDuration: Long = 0,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val request = FakeRequest(this, responses, requestDuration)
        val poller = DefaultCheckoutSessionPoller(request::poll) { testScheduler.currentTime }
        Scenario(this, request, poller).block()
        request.calls.ensureAllEventsConsumed()
    }

    private data class Scenario(val scope: TestScope, val request: FakeRequest, val poller: CheckoutSessionPoller)

    private class FakeRequest(
        private val scope: TestScope,
        responses: List<Result<CheckoutSessionPollResponse>>,
        private val duration: Long,
    ) {
        val calls = Turbine<Call>()
        private val responses = ArrayDeque(responses)
        suspend fun poll(id: String, timeoutMillis: Long): Result<CheckoutSessionPollResponse> {
            calls.add(Call(id, scope.testScheduler.currentTime, timeoutMillis))
            delay(duration)
            return responses.removeFirstOrNull() ?: success(CheckoutSessionPollResponse.State.ACTIVE)
        }
    }

    private data class Call(val id: String, val startedAt: Long, val timeoutMillis: Long)

    enum class TerminalState(val state: CheckoutSessionPollResponse.State, val outcome: CheckoutSessionPoller.Outcome) {
        SUCCEEDED(CheckoutSessionPollResponse.State.SUCCEEDED, CheckoutSessionPoller.Outcome.COMPLETED),
        ASYNC_PAYMENT(
            CheckoutSessionPollResponse.State.PROCESSING_ASYNC_PAYMENT,
            CheckoutSessionPoller.Outcome.COMPLETED,
        ),
        CUSTOMER_ACTION(
            CheckoutSessionPollResponse.State.PENDING_ASYNC_CUSTOMER_ACTION,
            CheckoutSessionPoller.Outcome.COMPLETED,
        ),
        FAILED(
            CheckoutSessionPollResponse.State.FAILED_ASYNC_PAYMENT,
            CheckoutSessionPoller.Outcome.FAILED_ASYNC_PAYMENT,
        ),
        INVALID(CheckoutSessionPollResponse.State.INVALID, CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED),
        EXPIRED(CheckoutSessionPollResponse.State.EXPIRED, CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED),
    }

    enum class InProgressState(val state: CheckoutSessionPollResponse.State) {
        ACTIVE(CheckoutSessionPollResponse.State.ACTIVE),
        PAYMENT(CheckoutSessionPollResponse.State.PROCESSING_SYNC_PAYMENT),
        SUBSCRIPTION(CheckoutSessionPollResponse.State.PROCESSING_SUBSCRIPTION),
    }

    private companion object {
        fun success(state: CheckoutSessionPollResponse.State) = Result.success(
            CheckoutSessionPollResponse(
                sessionId = "cs_test",
                state = state,
                paymentObjectStatus = null,
            )
        )
        fun rateLimit(): Result<CheckoutSessionPollResponse> = Result.failure(APIException(statusCode = 429))
    }
}
