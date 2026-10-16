package com.stripe.android.checkout.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutEventReporterTest {
    @Test
    fun `started and succeeded report checkout confirmation context and duration`() {
        val timeSource = TestTimeSource()
        val scenario = createScenario(timeSource)
        val params = mapOf(
            "checkout_session_id" to "cs_reporter_test",
            "selected_lpm" to "card",
        )

        val startedAt = scenario.reporter.started(params)
        timeSource += 1250.milliseconds
        scenario.reporter.succeeded(
            params = params,
            duration = startedAt.elapsedNow(),
        )

        val events = scenario.events()
        assertThat(events.map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        assertThat(events[0]).containsEntry("product_usage", "checkout_elements")
        assertThat(events[0]).containsEntry("checkout_session_id", "cs_reporter_test")
        assertThat(events[0]).doesNotContainKey("duration")
        assertThat(events[1]).containsEntry("selected_lpm", "card")
        assertThat(events[1]).doesNotContainKey("request_id")
        assertThat(events[1]).containsEntry("duration", 1.25f)
    }

    @Test
    fun `failed reports safe Stripe error fields without its message`() {
        val scenario = createScenario(TestTimeSource())
        val privateMessage = "private payment details"

        scenario.reporter.failed(
            params = mapOf("selected_lpm" to "card"),
            error = APIException(
                stripeError = StripeError(
                    type = "card_error",
                    code = "card_declined",
                    message = privateMessage,
                ),
                requestId = "req_failed_confirmation",
                message = privateMessage,
            ),
            duration = 500.milliseconds,
        )

        val event = scenario.events().single()
        assertThat(event["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(event).containsEntry("product_usage", "checkout_elements")
        assertThat(event).containsEntry("error_type", "card_error")
        assertThat(event).containsEntry("error_code", "card_declined")
        assertThat(event).containsEntry("request_id", "req_failed_confirmation")
        assertThat(event).containsEntry("duration", 0.5f)
        assertThat(event.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `canceled reports canceled phase without failure fields`() {
        val scenario = createScenario(TestTimeSource())

        scenario.reporter.canceled(
            params = mapOf("selected_lpm" to "link"),
            duration = 250.milliseconds,
        )

        val event = scenario.events().single()
        assertThat(event["event"]).isEqualTo("checkout_elements.confirm.canceled")
        assertThat(event).containsEntry("product_usage", "checkout_elements")
        assertThat(event).containsEntry("duration", 0.25f)
        assertThat(event).doesNotContainKey("request_id")
        assertThat(event).doesNotContainKey("error_type")
        assertThat(event).doesNotContainKey("error_code")
    }

    private fun createScenario(timeSource: TestTimeSource): Scenario {
        val executor = FakeAnalyticsRequestExecutor()
        return Scenario(
            reporter = CheckoutEventReporterTestFactory.create(
                analyticsRequestExecutor = executor,
                timeSource = timeSource,
            ),
            executor = executor,
        )
    }

    private data class Scenario(
        val reporter: CheckoutEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
    ) {
        fun events(): List<Map<String, *>> = executor.getExecutedRequests().map { it.params }
    }
}

@OptIn(ApiConfigurationPreview::class)
internal object CheckoutEventReporterTestFactory {
    fun create(
        analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        timeSource: TimeSource = TimeSource.Monotonic,
    ): CheckoutEventReporter {
        val analyticsRequestFactory = CheckoutAnalyticsModule.provideAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            apiConfigurationProvider = Provider {
                ApiConfiguration("pk_test_checkout_analytics").build()
            },
        )
        return CheckoutEventReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            timeSource = timeSource,
        )
    }
}
