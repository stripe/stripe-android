package com.stripe.android.checkout.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ServerUpdateOperationTest {
    @Test
    fun `succeeded reports merchant update and refresh durations in seconds`() = runScenario {
        operation.started(response = null)
        events.awaitItem()
        operation.measureMerchantUpdate { }
        operation.measureRefresh { }

        operation.succeeded(response = null)

        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.run_server_update.succeeded")
        assertThat(event.params["merchant_update_duration"] as Float).isAtLeast(0f)
        assertThat(event.params["refresh_duration"] as Float).isAtLeast(0f)
    }

    @Test
    fun `failed before refresh reports merchant update duration and error`() = runScenario {
        operation.started(response = null)
        events.awaitItem()
        operation.measureMerchantUpdate { }

        operation.failed(response = null, error = IllegalStateException("boom"))

        val event = events.awaitItem()
        assertThat(event.eventName).isEqualTo("checkout_elements.run_server_update.failed")
        assertThat(event.params["merchant_update_duration"] as Float).isAtLeast(0f)
        assertThat(event.params).doesNotContainKey("refresh_duration")
        assertThat(event.params["error_type"]).isEqualTo("java.lang.IllegalStateException")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val events = Turbine<CheckoutAnalyticsEvent>()
        Scenario(
            operation = ServerUpdateOperation(report = events::add),
            events = events,
        ).block()
        events.ensureAllEventsConsumed()
    }

    private class Scenario(
        val operation: ServerUpdateOperation,
        val events: Turbine<CheckoutAnalyticsEvent>,
    )
}
