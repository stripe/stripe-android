@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.checkout.analytics.toAnalyticsMap
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutInitOperationTest {
    @Test
    fun `init reports attempted identity then latest response and ordered methods on success`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_loaded", configId = "cfg_loaded")

        val result = reporter.measureInit(CLIENT_SECRET, CONFIGURATION) { operation ->
            val started = analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
            assertThat(started).containsEntry("checkout_session_id", "cs_test_attempted")
            assertThat(started).doesNotContainKey("checkout_config_id")
            assertThat(started).doesNotContainKey("duration")
            assertThat(started).doesNotContainKey("ordered_lpms")

            operation.recordResponse(response)
            operation.orderedLpms = listOf("card", "klarna")
            timeSource += 1250.milliseconds
            Result.success("loaded")
        }

        assertThat(result.getOrThrow()).isEqualTo("loaded")
        val succeeded = analyticsRequestExecutor.awaitEvent(INIT_SUCCEEDED_EVENT)
        assertThat(succeeded).containsEntry("checkout_session_id", "cs_test_loaded")
        assertThat(succeeded).containsEntry("checkout_config_id", "cfg_loaded")
        assertThat(succeeded).containsEntry("ordered_lpms", "card,klarna")
        assertThat(succeeded).containsEntry("duration", 1.25f)
    }

    @Test
    fun `init failure reports latest response without ordered methods or private error details`() = runScenario {
        val error = IllegalStateException("private failure details")
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_loaded", configId = "cfg_loaded")

        val result = reporter.measureInit(CLIENT_SECRET, CONFIGURATION) { operation ->
            analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
            operation.recordResponse(response)
            operation.orderedLpms = listOf("card")
            timeSource += 500.milliseconds
            Result.failure<Unit>(error)
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
        val failed = analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
        assertThat(failed).containsEntry("checkout_session_id", "cs_test_loaded")
        assertThat(failed).containsEntry("checkout_config_id", "cfg_loaded")
        assertThat(failed).containsEntry("error_type", "java.lang.IllegalStateException")
        assertThat(failed).containsEntry("duration", 0.5f)
        assertThat(failed).doesNotContainKey("ordered_lpms")
        assertThat(failed.toString()).doesNotContain("private failure details")
    }

    @Test
    fun `init omits session identity when the client secret has no separator`() = runScenario {
        reporter.measureInit("private-client-secret", CONFIGURATION) {
            Result.failure<Unit>(IllegalStateException("invalid secret"))
        }

        assertThat(analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)).doesNotContainKey("checkout_session_id")
        assertThat(analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)).doesNotContainKey("checkout_session_id")
    }

    @Test
    fun `init reports returned cancellation even when its caller was canceled`() = runScenario {
        val cancellation = CancellationException("canceled")
        var result: Result<Unit>? = null

        val caller = testScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            result = reporter.measureInit(CLIENT_SECRET, CONFIGURATION) {
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
    fun `init reports and rethrows thrown cancellation`() = runScenario {
        val cancellation = CancellationException("canceled while queued")

        val thrown = runCatching {
            reporter.measureInit<Unit>(CLIENT_SECRET, CONFIGURATION) { throw cancellation }
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(cancellation)
        analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT)
        analyticsRequestExecutor.awaitEvent(INIT_FAILED_EVENT)
    }

    @Test
    fun `attempted configuration overrides the published configuration`() {
        val publishedConfiguration = CheckoutControllerStateFactory.create().configuration
        val attemptedConfiguration = CheckoutController.Configuration()
            .merchantDisplayName("Attempted merchant")
            .build()
        assertThat(publishedConfiguration.merchantDisplayName).isNull()

        runScenario(configuration = publishedConfiguration) {
            reporter.measureInit(CLIENT_SECRET, attemptedConfiguration) { Result.success(Unit) }

            assertThat(analyticsRequestExecutor.awaitEvent(INIT_STARTED_EVENT))
                .containsEntry("merchant_display_name", true)
            assertThat(analyticsRequestExecutor.awaitEvent(INIT_SUCCEEDED_EVENT))
                .containsEntry("merchant_display_name", true)
        }
    }

    private fun runScenario(
        configuration: CheckoutController.Configuration.State? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val timeSource = TestTimeSource()
        val reporter = TestCheckoutOperationReporterFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            workContext = UnconfinedTestDispatcher(testScheduler),
            configurationParams = { configuration?.toAnalyticsMap().orEmpty() },
            timeSource = timeSource,
        )

        Scenario(reporter, analyticsRequestExecutor, timeSource, this).block()
        analyticsRequestExecutor.validate()
    }

    private class Scenario(
        val reporter: CheckoutOperationReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
        val testScope: TestScope,
    )

    private companion object {
        const val CLIENT_SECRET = "cs_test_attempted_secret_example"
        val CONFIGURATION = CheckoutController.Configuration().build()
        const val INIT_STARTED_EVENT = "checkout_elements.init.started"
        const val INIT_SUCCEEDED_EVENT = "checkout_elements.init.succeeded"
        const val INIT_FAILED_EVENT = "checkout_elements.init.failed"
    }
}
