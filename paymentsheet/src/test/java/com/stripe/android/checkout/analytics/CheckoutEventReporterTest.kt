package com.stripe.android.checkout.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

internal class CheckoutEventReporterTest {
    @Test
    fun markNowUsesInjectedTimeSource() {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)

        val mark = scenario.reporter.markNow()
        timeSource += 500.milliseconds

        assertThat(mark.elapsedNow()).isEqualTo(500.milliseconds)
        assertThat(scenario.executor.getExecutedRequests()).isEmpty()
    }

    @Test
    fun startedSnapshotsContextAndSuccessUsesCurrentContext() {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)
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
            "duration" to 99.0f,
        )

        val startedAt = scenario.reporter.started(
            operation = CheckoutAnalyticsOperation.Init,
            response = response,
            additionalParams = additionalParams,
        )

        metadata["provider"] = "after_started"
        additionalParams["selected_lpm"] = "link"
        timeSource += 750.milliseconds
        scenario.reporter.succeeded(
            operation = CheckoutAnalyticsOperation.Init,
            response = response,
            requestId = "req_current",
            duration = startedAt.elapsedNow(),
            additionalParams = additionalParams,
        )
        metadata["provider"] = "after_succeeded"
        additionalParams["selected_lpm"] = "after_succeeded"

        val events = scenario.events()
        val started = events[0]
        val succeeded = events[1]

        assertThat(started[FIELD_EVENT]).isEqualTo("checkout_elements.init.started")
        assertThat(started).containsEntry("checkout_session_id", "cs_test_snapshot")
        assertThat(started).containsEntry("checkout_config_id", "cfg_test_snapshot")
        assertThat(started).containsEntry("selected_lpm", "card")
        assertThat(started["metadata"]).isEqualTo(mapOf("provider" to "initial"))
        assertThat(started).doesNotContainKey("duration")
        assertThat(started).doesNotContainKey("ignored_null")
        assertThat(started).doesNotContainKey("request_id")

        assertThat(succeeded[FIELD_EVENT]).isEqualTo("checkout_elements.init.succeeded")
        assertThat(succeeded).containsEntry("selected_lpm", "link")
        assertThat(succeeded["metadata"]).isEqualTo(mapOf("provider" to "after_started"))
        assertThat(succeeded).containsEntry("request_id", "req_current")
        assertThat(succeeded).containsEntry("duration", 0.75f)
    }

    @Test
    fun nullTerminalInputsDoNotRestoreRequestIdOrDuration() {
        val scenario = createScenario(TestTimeSource())
        val response = CheckoutSessionResponseFactory.create(requestId = "req_response")

        scenario.reporter.started(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = response,
            additionalParams = mapOf(
                "request_id" to "req_started",
                "duration" to 12.0f,
            ),
        )
        scenario.reporter.succeeded(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = response,
            requestId = null,
            duration = null,
            additionalParams = mapOf(
                "request_id" to "req_additional",
                "duration" to 12.0f,
            ),
        )

        val events = scenario.events()
        assertThat(events).hasSize(2)
        assertThat(events[0][FIELD_EVENT]).isEqualTo("checkout_elements.confirm.started")
        assertThat(events[0]).doesNotContainKey("request_id")
        assertThat(events[0]).doesNotContainKey("duration")
        assertThat(events[1][FIELD_EVENT]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(events[1]).doesNotContainKey("request_id")
        assertThat(events[1]).doesNotContainKey("duration")
    }

    @Test
    fun failedStripeExceptionReportsSafeFieldsAndOverridesPriorRequestId() {
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

        scenario.reporter.failed(
            operation = CheckoutAnalyticsOperation.Confirm,
            error = error,
            response = null,
            requestId = "req_prior",
            duration = 1250.milliseconds,
            additionalParams = emptyMap(),
        )

        val params = scenario.events().single()
        assertThat(params[FIELD_EVENT]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(params).containsEntry("error_type", "card_error")
        assertThat(params).containsEntry("error_code", "card_declined")
        assertThat(params).containsEntry("request_id", "req_failure")
        assertThat(params).containsEntry("duration", 1.25f)
        assertThat(params).doesNotContainKey("error_message")
        assertThat(params.values).doesNotContain(message)
    }

    @Test
    fun failedStripeExceptionWithoutStripeErrorUsesClassNameAndPriorRequestId() {
        val scenario = createScenario(TestTimeSource())

        scenario.reporter.failed(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            error = APIException(
                stripeError = null,
                requestId = null,
                message = "private details",
            ),
            response = null,
            requestId = "req_previous_update",
            duration = null,
            additionalParams = emptyMap(),
        )

        val params = scenario.events().single()
        assertThat(params).containsEntry("error_type", "APIException")
        assertThat(params).containsEntry("request_id", "req_previous_update")
        assertThat(params).doesNotContainKey("error_code")
        assertThat(params).doesNotContainKey("duration")
        assertThat(params.values).doesNotContain("private details")
    }

    @Test
    fun failedNonStripeErrorUsesClassNameWithoutMessage() {
        val scenario = createScenario(TestTimeSource())
        val message = "contains private data"

        scenario.reporter.failed(
            operation = CheckoutAnalyticsOperation.UpdateCurrency,
            error = IllegalStateException(message),
            response = null,
            requestId = "req_previous_update",
            duration = 2.seconds,
            additionalParams = emptyMap(),
        )

        val params = scenario.events().single()
        assertThat(params).containsEntry("error_type", "IllegalStateException")
        assertThat(params).containsEntry("request_id", "req_previous_update")
        assertThat(params).containsEntry("duration", 2.0f)
        assertThat(params).doesNotContainKey("error_code")
        assertThat(params).doesNotContainKey("error_message")
        assertThat(params.values).doesNotContain(message)
    }

    @Test
    fun canceledUsesCanceledEventWithoutRestoringDurationOrRequestId() {
        val scenario = createScenario(TestTimeSource())
        val response = CheckoutSessionResponseFactory.create(
            id = "cs_test_canceled",
            requestId = "req_response",
        )

        scenario.reporter.canceled(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = response,
            duration = null,
            additionalParams = emptyMap(),
        )

        val params = scenario.events().single()
        assertThat(params[FIELD_EVENT]).isEqualTo("checkout_elements.confirm.canceled")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_canceled")
        assertThat(params).doesNotContainKey("request_id")
        assertThat(params).doesNotContainKey("duration")
        assertThat(params).doesNotContainKey("error_type")
        assertThat(params).doesNotContainKey("error_code")
    }

    @Test
    fun clearPaymentOptionReportsOneCheckoutEvent() {
        val scenario = createScenario(TestTimeSource())

        scenario.reporter.reportClearPaymentOption(
            CheckoutSessionResponseFactory.create(id = "cs_test_clear")
        )

        val params = scenario.events().single()
        assertThat(params[FIELD_EVENT]).isEqualTo("checkout_elements.clear_payment_option")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_clear")
        assertThat(params).doesNotContainKey("duration")
    }

    @Test
    fun operationsUseSnakeCaseValues() {
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
    fun invocationSourceValuesAreStableAndUnspecifiedIsOmitted() {
        assertThat(CheckoutInvocationSource.Merchant.value).isEqualTo("merchant")
        assertThat(CheckoutInvocationSource.PaymentElement.value).isEqualTo("payment_element")
        assertThat(CheckoutInvocationSource.ShippingAddressElement.value)
            .isEqualTo("shipping_address_element")
        assertThat(CheckoutInvocationSource.ExpressCheckoutElement.value)
            .isEqualTo("express_checkout_element")
        assertThat(CheckoutInvocationSource.Unspecified.value).isNull()

        val scenario = createScenario(TestTimeSource())
        scenario.reporter.started(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            response = null,
            additionalParams = mapOf("invocation_source" to CheckoutInvocationSource.Unspecified.value),
        )

        val params = scenario.events().single()
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
    ) {
        fun events(): List<Map<String, *>> = executor.getExecutedRequests().map { it.params }
    }

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
