@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.checkout.analytics.analyticsParams
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutOperationReporterTest {
    @Test
    fun `measure reports started and succeeded with the latest response and completion params`() = runScenario {
        val startingResponse = CheckoutSessionResponseFactory.create(id = "cs_test_starting", configId = "cfg_starting")
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_measured", configId = "cfg_measured")

        val result = reporter.measure(
            operation = Operation.RunServerUpdate,
            params = emptyMap(),
            response = startingResponse,
        ) { operation ->
            val started = analyticsRequestExecutor.awaitEvent(SERVER_STARTED_EVENT)
            assertThat(started).containsEntry("checkout_session_id", "cs_test_starting")
            assertThat(started).doesNotContainKey("duration")
            assertThat(started).doesNotContainKey("merchant_update_duration")

            operation.recordResponse(response)
            operation.putCompletionParam("merchant_update_duration", 1.0f)
            timeSource += 1250.milliseconds
            Result.success("loaded")
        }

        assertThat(result.getOrThrow()).isEqualTo("loaded")
        val succeeded = analyticsRequestExecutor.awaitEvent(SERVER_SUCCEEDED_EVENT)
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_measured")
        assertThat(succeeded).containsEntry("checkout_config_id", "cfg_measured")
        assertThat(succeeded).containsEntry("merchant_update_duration", 1.0f)
        assertThat(succeeded).containsEntry("duration", 1.25f)
    }

    @Test
    fun `measure reports the starting session when the operation fails before a new response`() = runScenario {
        val startingResponse = CheckoutSessionResponseFactory.create(id = "cs_test_starting", configId = "cfg_starting")

        reporter.measure(
            operation = Operation.ApplyPromotionCode,
            params = emptyMap(),
            response = startingResponse,
        ) {
            Result.failure<Unit>(IllegalStateException("promotion code rejected"))
        }

        val started = analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.started")
        assertThat(started).containsEntry("checkout_session_id", "cs_test_starting")
        assertThat(started).containsEntry("checkout_config_id", "cfg_starting")
        val failed = analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.failed")
        assertThat(failed).containsEntry("checkout_session_id", "cs_test_starting")
        assertThat(failed).containsEntry("checkout_config_id", "cfg_starting")
    }

    @Test
    fun `reportCompleted reports started without the tax update flag then succeeded with the default`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_completed", configId = "cfg_completed")

        reporter.reportCompleted(
            operation = Operation.UpdateBillingAddress,
            params = mapOf("invocation_source" to "payment_element"),
            response = response,
        )

        val started = analyticsRequestExecutor.awaitEvent("checkout_elements.update_billing_address.started")
        assertThat(started).containsEntry("invocation_source", "payment_element")
        assertThat(started).containsEntry("checkout_session_id", "cs_test_completed")
        assertThat(started).doesNotContainKey("did_request_tax_update")
        assertThat(started).doesNotContainKey("duration")
        val succeeded = analyticsRequestExecutor.awaitEvent("checkout_elements.update_billing_address.succeeded")
        assertThat(succeeded).containsEntry("invocation_source", "payment_element")
        assertThat(succeeded).containsEntry("did_request_tax_update", false)
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_completed")
    }

    @Test
    fun `address operation rejected before work reports the tax update flag as false when failed`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_rejected")

        reporter.measure(
            operation = Operation.UpdateShippingAddress,
            params = InvocationSource.Merchant.analyticsParams(),
            response = response,
        ) {
            Result.failure<Unit>(IllegalStateException("Cannot mutate checkout session while presented."))
        }

        val started = analyticsRequestExecutor.awaitEvent("checkout_elements.update_shipping_address.started")
        assertThat(started).doesNotContainKey("did_request_tax_update")
        val failed = analyticsRequestExecutor.awaitEvent("checkout_elements.update_shipping_address.failed")
        assertThat(failed).containsEntry("invocation_source", "merchant")
        assertThat(failed).containsEntry("did_request_tax_update", false)
    }

    @Test
    fun `operations without completion defaults do not report the tax update flag`() = runScenario {
        reporter.measure(
            operation = Operation.UpdateEmail,
            response = CheckoutSessionResponseFactory.create(),
        ) {
            Result.success(Unit)
        }

        analyticsRequestExecutor.awaitEvent("checkout_elements.update_email.started")
        val succeeded = analyticsRequestExecutor.awaitEvent("checkout_elements.update_email.succeeded")
        assertThat(succeeded).doesNotContainKey("did_request_tax_update")
    }

    @Test
    fun `reportEvent reports a single event with session metadata and no duration`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_single", configId = "cfg_single")

        reporter.reportEvent(Operation.ClearPaymentOption, response)

        val event = analyticsRequestExecutor.awaitEvent("checkout_elements.clear_payment_option")
        assertThat(event).containsEntry("checkout_session_id", "cs_test_single")
        assertThat(event).containsEntry("checkout_config_id", "cfg_single")
        assertThat(event).containsEntry("publishable_key", PUBLISHABLE_KEY)
        assertThat(event).containsEntry("product_usage", "checkout_elements")
        assertThat(event).doesNotContainKey("duration")
        assertThat(event).doesNotContainKey("error_type")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val timeSource = TestTimeSource()
        val reporter = TestCheckoutOperationReporterFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            workContext = UnconfinedTestDispatcher(testScheduler),
            publishableKey = { PUBLISHABLE_KEY },
            timeSource = timeSource,
        )

        Scenario(
            reporter = reporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
        ).block()

        analyticsRequestExecutor.validate()
    }

    private class Scenario(
        val reporter: CheckoutOperationReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
    )

    private companion object {
        const val PUBLISHABLE_KEY = "pk_test_operation_reporter"

        const val SERVER_STARTED_EVENT = "checkout_elements.run_server_update.started"
        const val SERVER_SUCCEEDED_EVENT = "checkout_elements.run_server_update.succeeded"
    }
}
