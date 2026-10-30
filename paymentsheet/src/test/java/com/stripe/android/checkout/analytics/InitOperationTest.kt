package com.stripe.android.checkout.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CancellationException
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

    @Test
    fun `perform reports success with the latest response`() = runScenario(
        sessionId = "cs_test_attempt",
    ) {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_test_initial",
            configId = "config_initial",
            currency = "usd",
        )
        val latestResponse = CheckoutSessionResponseFactory.create(
            id = "cs_test_latest",
            configId = "config_after_tax",
            currency = "eur",
        )

        val result = operation.perform {
            assertThat(events.awaitItem().eventName).isEqualTo("checkout_elements.init.started")
            operation.updateResponse(initialResponse)
            operation.updateResponse(latestResponse)
            timeSource += 1250.milliseconds
            Result.success(listOf("paypal", "card"))
        }

        assertThat(result.getOrThrow()).containsExactly("paypal", "card")
        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.init.succeeded")
        assertThat(event.params).containsEntry("checkout_config_id", "config_after_tax")
        assertThat(event.params).containsEntry("currency", "eur")
        assertThat(event.params).containsEntry("ordered_lpms", "paypal,card")
        assertThat(event.params).containsEntry("duration", 1.25f)
    }

    @Test
    fun `perform reports failure with the latest response`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_test_initial",
            configId = "config_initial",
            currency = "usd",
        )
        val latestResponse = CheckoutSessionResponseFactory.create(
            id = "cs_test_latest",
            configId = "config_before_failure",
            currency = "eur",
        )
        val expectedError = APIException(
            stripeError = StripeError(type = "invalid_request_error", code = "resource_missing"),
            requestId = "req_init_failed",
            message = "private integration details",
        )

        val result = operation.perform {
            assertThat(events.awaitItem().eventName).isEqualTo("checkout_elements.init.started")
            operation.updateResponse(initialResponse)
            operation.updateResponse(latestResponse)
            timeSource += 500.milliseconds
            Result.failure(expectedError)
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedError)
        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.init.failed")
        assertThat(event.params).containsEntry("checkout_config_id", "config_before_failure")
        assertThat(event.params).containsEntry("currency", "eur")
        assertThat(event.params).containsEntry("request_id", "req_init_failed")
        assertThat(event.params).doesNotContainKey("ordered_lpms")
    }

    @Test
    fun `perform failure before a response includes only the attempted session identity`() =
        runScenario(sessionId = "cs_test_new_attempt") {
            val expectedError = IllegalArgumentException("Invalid configure arguments")

            val result = operation.perform {
                assertThat(events.awaitItem().eventName).isEqualTo("checkout_elements.init.started")
                Result.failure(expectedError)
            }

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedError)
            val event = events.awaitItem()
            assertThat(event.eventName).isEqualTo("checkout_elements.init.failed")
            assertThat(event.params).containsEntry("checkout_session_id", "cs_test_new_attempt")
            assertThat(event.params).doesNotContainKey("checkout_config_id")
            assertThat(event.params).doesNotContainKey("currency")
        }

    @Test
    fun `perform propagates cancellation failure without reporting failed outcome`() = runScenario {
        val expectedCancellation = CancellationException("init canceled")

        val actualCancellation = try {
            operation.perform {
                Result.failure<List<String>>(expectedCancellation)
            }
            error("perform should rethrow cancellation")
        } catch (error: CancellationException) {
            error
        }

        assertThat(actualCancellation).isSameInstanceAs(expectedCancellation)
        assertThat(events.awaitItem().eventName).isEqualTo("checkout_elements.init.started")
        events.expectNoEvents()
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
