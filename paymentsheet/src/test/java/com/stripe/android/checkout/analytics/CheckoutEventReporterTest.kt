package com.stripe.android.checkout.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

internal class CheckoutEventReporterTest {
    @Test
    fun `start snapshots context and excludes response request id`() {
        val scenario = createScenario(TestTimeSource())
        val response = CheckoutSessionResponseFactory.create(
            id = "cs_test_snapshot",
            configId = "cfg_test_snapshot",
            requestId = "req_from_response",
        )
        val metadata = mutableMapOf("provider" to "initial")
        val additionalParams = mutableMapOf<String, Any?>(
            "selected_lpm" to "card",
            "metadata" to metadata,
            "ignored_null" to null,
            "request_id" to "req_stale",
            "product_usage" to "PaymentElement",
        )

        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Init,
            response = response,
            additionalParams = additionalParams,
        )

        metadata["provider"] = "mutated"
        additionalParams["selected_lpm"] = "link"
        span.recordRequestId("req_from_response")
        span.addParams(mapOf("did_request_tax_update" to true))
        span.succeeded(
            response = response,
            requestId = "req_current",
            additionalParams = emptyMap(),
        )

        val requests = scenario.executor.getExecutedRequests()
        val started = requests[0].params
        val succeeded = requests[1].params

        assertThat(started[FIELD_EVENT]).isEqualTo("checkout_elements.init.started")
        assertThat(started).containsEntry("checkout_session_id", "cs_test_snapshot")
        assertThat(started).containsEntry("checkout_config_id", "cfg_test_snapshot")
        assertThat(started).containsEntry("selected_lpm", "card")
        assertThat(started["metadata"]).isEqualTo(mapOf("provider" to "initial"))
        assertThat(started).doesNotContainKey("duration")
        assertThat(started).doesNotContainKey("ignored_null")
        assertThat(started).doesNotContainKey("request_id")
        assertThat(started).containsEntry("product_usage", "checkout_elements")

        assertThat(succeeded[FIELD_EVENT]).isEqualTo("checkout_elements.init.succeeded")
        assertThat(succeeded).containsEntry("selected_lpm", "card")
        assertThat(succeeded).containsEntry("did_request_tax_update", true)
        assertThat(succeeded).containsEntry("request_id", "req_current")
        assertThat(succeeded).containsEntry("product_usage", "checkout_elements")
    }

    @Test
    fun `interleaved spans retain independent total durations`() {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)

        val first = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Init,
            response = null,
            additionalParams = emptyMap(),
        )
        timeSource += 2.seconds
        val second = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = null,
            additionalParams = emptyMap(),
        )
        timeSource += 3.seconds

        first.succeeded(null, "req_first", emptyMap())
        second.failed(IllegalStateException("private error"), null, emptyMap())

        val events = scenario.executor.getExecutedRequests().map { it.params }
        assertThat(events[0]).doesNotContainKey("duration")
        assertThat(events[1]).containsEntry("duration", 5.0f)
        assertThat(events[1][FIELD_EVENT]).isEqualTo("checkout_elements.init.succeeded")
        assertThat(events[2]).containsEntry("duration", 3.0f)
        assertThat(events[2][FIELD_EVENT]).isEqualTo("checkout_elements.confirm.failed")
    }

    @Test
    fun `resumed span reports terminal event without start or duration`() {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)
        val span = scenario.reporter.resume(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = CheckoutSessionResponseFactory.create(),
            additionalParams = mapOf("selected_lpm" to "card"),
        )
        timeSource += 5.seconds

        span.succeeded(null, "req_confirm", emptyMap())

        val params = scenario.executor.getExecutedRequests().single().params
        assertThat(params[FIELD_EVENT]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(params).containsEntry("request_id", "req_confirm")
        assertThat(params).doesNotContainKey("duration")
    }

    @Test
    fun `recorded response request id is included when terminal has no request id`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Init,
            response = null,
            additionalParams = emptyMap(),
        )

        span.recordRequestId("req_init")
        span.succeeded(response = null, requestId = null, additionalParams = emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("request_id", "req_init")
    }

    @Test
    fun `later response without request id clears earlier recorded request id`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            response = null,
            additionalParams = emptyMap(),
        )
        span.recordRequestId("req_init")
        span.recordRequestId(null)

        span.succeeded(response = null, requestId = null, additionalParams = emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).doesNotContainKey("request_id")
    }

    @Test
    fun `failed span reports structured Stripe error without message`() {
        val scenario = createScenario(TestTimeSource())
        val message = "secret payment details"
        val error = APIException(
            stripeError = StripeError(
                type = "card_error",
                code = "card_declined",
                message = message,
            ),
            requestId = "req_failure",
            message = message,
        )
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = null,
            additionalParams = emptyMap(),
        )

        span.failed(error, null, emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("error_type", "card_error")
        assertThat(params).containsEntry("error_code", "card_declined")
        assertThat(params).containsEntry("request_id", "req_failure")
        assertThat(params).doesNotContainKey("error_message")
        assertThat(params.values).doesNotContain(message)
    }

    @Test
    fun `Stripe exception request id supersedes recorded response request id`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            response = null,
            additionalParams = emptyMap(),
        )
        span.recordRequestId("req_init")

        span.failed(
            APIException(
                stripeError = StripeError(type = "api_error"),
                requestId = "req_tax_update",
                message = "private details",
            ),
            response = null,
            additionalParams = emptyMap(),
        )

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("request_id", "req_tax_update")
        assertThat(params.values).doesNotContain("private details")
    }

    @Test
    fun `failed Stripe exception without Stripe error uses class name as type`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            response = null,
            additionalParams = emptyMap(),
        )

        span.failed(
            APIException(
                stripeError = null,
                requestId = "req_failure",
                message = "private details",
            ),
            response = null,
            additionalParams = emptyMap(),
        )

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("error_type", "APIException")
        assertThat(params).containsEntry("request_id", "req_failure")
        assertThat(params).doesNotContainKey("error_code")
        assertThat(params.values).doesNotContain("private details")
    }

    @Test
    fun `failed span classifies non Stripe errors without message`() {
        val scenario = createScenario(TestTimeSource())
        val message = "contains private data"
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.UpdateCurrency,
            response = null,
            additionalParams = emptyMap(),
        )

        span.failed(IllegalStateException(message), null, emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("error_type", "IllegalStateException")
        assertThat(params).doesNotContainKey("error_code")
        assertThat(params).doesNotContainKey("error_message")
        assertThat(params.values).doesNotContain(message)
    }

    @Test
    fun `measure duration is attached to terminal event when block throws`() = runTest {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)
        val error = IllegalStateException("do not report this")
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            response = null,
            additionalParams = emptyMap(),
        )

        var caught: Throwable? = null
        try {
            span.measure("merchant_update_duration") {
                timeSource += 1.25.seconds
                throw error
            }
        } catch (throwable: IllegalStateException) {
            caught = throwable
        }

        assertThat(caught).isSameInstanceAs(error)
        span.failed(error, null, emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("merchant_update_duration", 1.25f)
        assertThat(params).containsEntry("duration", 1.25f)
        assertThat(params.values).doesNotContain("do not report this")
    }

    @Test
    fun `measure records separate durations on the owning span`() = runTest {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            response = null,
            additionalParams = emptyMap(),
        )

        span.measure("merchant_update_duration") {
            timeSource += 750.milliseconds
        }
        span.measure("refresh_duration") {
            timeSource += 1.25.seconds
        }
        span.succeeded(null, "req_update", emptyMap())

        val params = scenario.executor.getExecutedRequests().last().params
        assertThat(params).containsEntry("merchant_update_duration", 0.75f)
        assertThat(params).containsEntry("refresh_duration", 1.25f)
        assertThat(params).containsEntry("duration", 2.0f)
    }

    @Test
    fun `canceled span uses canceled event name`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = null,
            additionalParams = emptyMap(),
        )

        span.canceled(null, emptyMap())

        assertThat(scenario.executor.getExecutedRequests().last().params[FIELD_EVENT])
            .isEqualTo("checkout_elements.confirm.canceled")
    }

    @Test
    fun `duplicate terminal calls emit only one terminal event`() {
        val scenario = createScenario(TestTimeSource())
        val span = scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.ApplyPromotionCode,
            response = null,
            additionalParams = emptyMap(),
        )

        span.succeeded(null, null, emptyMap())
        span.failed(IllegalStateException("late failure"), null, emptyMap())
        span.canceled(null, emptyMap())

        val events = scenario.executor.getExecutedRequests().map { it.params[FIELD_EVENT] }
        assertThat(events).containsExactly(
            "checkout_elements.apply_promotion_code.started",
            "checkout_elements.apply_promotion_code.succeeded",
        ).inOrder()
    }

    @Test
    fun `clear payment option reports one checkout event`() {
        val scenario = createScenario(TestTimeSource())

        scenario.reporter.reportClearPaymentOption(
            CheckoutSessionResponseFactory.create(id = "cs_test_clear")
        )

        val params = scenario.executor.getExecutedRequests().single().params
        assertThat(params[FIELD_EVENT]).isEqualTo("checkout_elements.clear_payment_option")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_clear")
        assertThat(params).containsEntry("product_usage", "checkout_elements")
        assertThat(params).doesNotContainKey("duration")
    }

    @Test
    fun `operations use snake case values`() {
        assertThat(CheckoutAnalyticsOperation.values().map { it.value }).containsExactly(
            "init",
            "confirm",
            "apply_promotion_code",
            "remove_promotion_code",
            "update_shipping_address",
            "update_billing_address",
            "update_currency",
            "run_server_update",
        ).inOrder()
    }

    @Test
    fun `invocation source values are stable and unspecified is omitted`() {
        assertThat(CheckoutInvocationSource.Merchant.value).isEqualTo("merchant")
        assertThat(CheckoutInvocationSource.PaymentElement.value).isEqualTo("payment_element")
        assertThat(CheckoutInvocationSource.ShippingAddressElement.value)
            .isEqualTo("shipping_address_element")
        assertThat(CheckoutInvocationSource.ExpressCheckoutElement.value)
            .isEqualTo("express_checkout_element")
        assertThat(CheckoutInvocationSource.Unspecified.value).isNull()

        val scenario = createScenario(TestTimeSource())
        scenario.reporter.start(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            response = null,
            additionalParams = mapOf("invocation_source" to CheckoutInvocationSource.Unspecified.value),
        )

        val params = scenario.executor.getExecutedRequests().single().params
        assertThat(params).doesNotContainKey("invocation_source")
    }

    private fun createScenario(timeSource: TimeSource): Scenario {
        val executor = FakeAnalyticsRequestExecutor()
        return Scenario(
            reporter = CheckoutEventReporterFactory.create(executor, timeSource),
            executor = executor,
        )
    }

    private data class Scenario(
        val reporter: CheckoutEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
    )

    private companion object {
        const val FIELD_EVENT = "event"
    }
}

internal object CheckoutEventReporterFactory {
    fun create(
        analyticsRequestExecutor: FakeAnalyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
        timeSource: TimeSource = TimeSource.Monotonic,
    ): CheckoutEventReporter = CheckoutEventReporter(
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = AnalyticsRequestFactory(
            packageManager = null,
            packageInfo = null,
            packageName = "",
            publishableKeyProvider = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY },
            networkTypeProvider = { "5G" },
            pluginTypeProvider = { null },
        ),
        timeSource = timeSource,
    )
}
