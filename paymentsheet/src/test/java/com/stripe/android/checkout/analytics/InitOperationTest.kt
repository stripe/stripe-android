package com.stripe.android.checkout.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

internal class InitOperationTest {
    @Test
    fun `started includes session identity without terminal fields`() = runScenario {
        operation.started(response = null)

        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.init.started")
        assertThat(event.params).containsExactly(
            "checkout_session_id", "cs_test_init",
        )
    }

    @Test
    fun `events omit unavailable session identity before a response`() = runScenario(sessionId = null) {
        operation.started(response = null)
        timeSource += 100.milliseconds
        operation.failed(response = null, error = IllegalArgumentException("invalid client secret"))

        assertThat(events.awaitItem().params).isEmpty()
        assertThat(events.awaitItem().params).containsExactly(
            "duration", 0.1f,
            "error_type", "invalidRequestError",
        )
    }

    @Test
    fun `succeeded serializes resolved payment methods in display order and duration in seconds`() = runScenario(
        sessionId = "cs_test_attempt",
    ) {
        timeSource += 1250.milliseconds
        operation.succeeded(
            response = CheckoutSessionResponseFactory.create(id = "cs_test_init", configId = "config_loaded"),
            orderedLpms = listOf("paypal", "card", "klarna"),
        )

        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.init.succeeded")
        assertThat(event.params).containsEntry("ordered_lpms", "paypal,card,klarna")
        assertThat(event.params).containsEntry("duration", 1.25f)
        assertThat(event.params).containsEntry("checkout_config_id", "config_loaded")
        assertThat(event.params).containsEntry("checkout_session_id", "cs_test_init")
        assertThat(event.params).doesNotContainKey("request_id")
    }

    @Test
    fun `API failure includes safe error fields and the current response`() = runScenario {
        val privateMessage = "private customer details"
        timeSource += 500.milliseconds
        operation.failed(
            response = CheckoutSessionResponseFactory.create(
                id = "cs_test_init",
                configId = "config_before_tax_failure",
                currency = "eur",
            ),
            error = APIException(
                stripeError = StripeError(type = "invalid_request_error", code = "resource_missing"),
                requestId = "req_failed_init",
                message = privateMessage,
            ),
        )

        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.init.failed")
        assertThat(event.params).containsEntry("checkout_config_id", "config_before_tax_failure")
        assertThat(event.params).containsEntry("currency", "eur")
        assertThat(event.params).containsEntry("duration", 0.5f)
        assertThat(event.params).containsEntry("error_type", "invalid_request_error")
        assertThat(event.params).containsEntry("error_code", "resource_missing")
        assertThat(event.params).containsEntry("request_id", "req_failed_init")
        assertThat(event.params).doesNotContainKey("ordered_lpms")
        assertThat(event.params.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `failure before init response retains only attempted session identity`() = runScenario(
        sessionId = "cs_test_new_attempt",
    ) {
        timeSource += 100.milliseconds
        operation.failed(response = null, error = IllegalStateException("private integration details"))

        assertThat(events.awaitItem().params).containsExactly(
            "checkout_session_id", "cs_test_new_attempt",
            "duration", 0.1f,
            "error_type", "java.lang.IllegalStateException",
        )
    }

    private fun runScenario(
        sessionId: String? = "cs_test_init",
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val timeSource = TestTimeSource()
        val events = Turbine<CheckoutAnalyticsEvent>()
        val operation = InitOperation(
            sessionId = sessionId,
            startedAt = timeSource.markNow(),
            report = { events.add(it) },
        )

        Scenario(
            operation = operation,
            timeSource = timeSource,
            events = events,
        ).apply { block() }

        events.ensureAllEventsConsumed()
    }

    private class Scenario(
        val operation: InitOperation,
        val timeSource: TestTimeSource,
        val events: Turbine<CheckoutAnalyticsEvent>,
    )
}
