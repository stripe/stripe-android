@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.checkout.analytics.toAnalyticsMap
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutOperationReporterTest {
    @Test
    fun `measure reports started and succeeded with the latest response and completion params`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_measured", configId = "cfg_measured")

        val result = reporter.measure(
            operation = Operation.Init,
            params = mapOf("checkout_session_id" to "cs_test_attempted"),
            response = null,
        ) { operation ->
            val started = analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
            assertThat(started).containsEntry("checkout_session_id", "cs_test_attempted")
            assertThat(started).doesNotContainKey("duration")
            assertThat(started).doesNotContainKey("ordered_lpms")

            operation.recordResponse(response)
            operation.putCompletionParam("ordered_lpms", "card,klarna")
            timeSource += 1250.milliseconds
            Result.success("loaded")
        }

        assertThat(result.getOrThrow()).isEqualTo("loaded")
        val succeeded = analyticsRequestExecutor.awaitEvent(INIT_SUCCEEDED_EVENT)
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_measured")
        assertThat(succeeded).containsEntry("checkout_config_id", "cfg_measured")
        assertThat(succeeded).containsEntry("ordered_lpms", "card,klarna")
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
    fun `measure reports failure with safe error fields`() = runScenario {
        val error = IllegalStateException("private failure details")

        val result = reporter.measure(Operation.Init, params = emptyMap(), response = null) {
            analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
            timeSource += 500.milliseconds
            Result.failure<Unit>(error)
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
        val failed = analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
        assertThat(failed).containsEntry("error_type", "java.lang.IllegalStateException")
        assertThat(failed).containsEntry("duration", 0.5f)
        assertThat(failed.toString()).doesNotContain("private failure details")
    }

    @Test
    fun `measure reports failed when its caller was canceled`() = runScenario {
        val cancellation = CancellationException("canceled")
        var result: Result<Unit>? = null

        val caller = testScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            result = reporter.measure(Operation.Init, params = emptyMap(), response = null) {
                currentCoroutineContext().cancel()
                Result.failure<Unit>(cancellation)
            }
        }
        caller.join()

        assertThat(result?.exceptionOrNull()).isSameInstanceAs(cancellation)
        analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
        analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
    }

    @Test
    fun `measure reports failed and rethrows a thrown cancellation`() = runScenario {
        val cancellation = CancellationException("canceled while queued")

        val thrown = runCatching {
            reporter.measure<Unit>(Operation.Init, params = emptyMap(), response = null) {
                throw cancellation
            }
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(cancellation)
        analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
        analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
    }

    @Test
    fun `measure reports failed for a timeout while its caller is still active`() = runScenario {
        val result = reporter.measure(Operation.Init, params = emptyMap(), response = null) {
            runCatching<Unit> { withTimeout(1.seconds) { awaitCancellation() } }
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(TimeoutCancellationException::class.java)
        analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
        val failed = analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
        assertThat(failed).containsKey("error_type")
    }

    @Test
    fun `reportCompleted reports started and succeeded with the session metadata`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_completed", configId = "cfg_completed")

        reporter.reportCompleted(
            operation = Operation.UpdateBillingAddress,
            params = emptyMap(),
            response = response,
        )

        val started = analyticsRequestExecutor.awaitEvent("checkout_elements.update_billing_address.started")
        assertThat(started).containsEntry("checkout_session_id", "cs_test_completed")
        assertThat(started).doesNotContainKey("duration")
        val succeeded = analyticsRequestExecutor.awaitEvent("checkout_elements.update_billing_address.succeeded")
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_completed")
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

    @Test
    fun `operation params override the published configuration`() {
        val publishedConfiguration = CheckoutElementsControllerStateFactory.create().configuration
        val attemptedConfiguration = CheckoutElementsController.Configuration()
            .merchantDisplayName("Attempted merchant")
            .build()
        assertThat(publishedConfiguration.merchantDisplayName).isNull()

        runScenario(configuration = publishedConfiguration) {
            reporter.measure(
                operation = Operation.Init,
                params = attemptedConfiguration.toAnalyticsMap(),
                response = null,
            ) {
                Result.success(Unit)
            }

            assertThat(analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT))
                .containsEntry("merchant_display_name", true)
            assertThat(analyticsRequestExecutor.awaitEvent(INIT_SUCCEEDED_EVENT))
                .containsEntry("merchant_display_name", true)
        }
    }

    private fun runScenario(
        configuration: CheckoutElementsController.Configuration.State? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val timeSource = TestTimeSource()
        val reporter = TestCheckoutOperationReporterFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            workContext = UnconfinedTestDispatcher(testScheduler),
            publishableKey = { PUBLISHABLE_KEY },
            configurationParams = { configuration?.toAnalyticsMap().orEmpty() },
            timeSource = timeSource,
        )

        Scenario(
            reporter = reporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
            testScope = this,
        ).block()

        analyticsRequestExecutor.validate()
    }

    private class Scenario(
        val reporter: CheckoutOperationReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
        val testScope: TestScope,
    )

    private companion object {
        const val PUBLISHABLE_KEY = "pk_test_operation_reporter"

        const val INIT_STARTED_EVENT = "checkout_elements.init.started"
        const val INIT_SUCCEEDED_EVENT = "checkout_elements.init.succeeded"
        const val INIT_FAILED_EVENT = "checkout_elements.init.failed"
    }
}
