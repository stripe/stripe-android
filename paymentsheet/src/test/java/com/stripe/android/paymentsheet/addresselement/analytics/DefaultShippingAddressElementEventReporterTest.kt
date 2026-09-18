package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test

internal class DefaultShippingAddressElementEventReporterTest {
    @Test
    fun `onShown reports the Checkout Session and address country`() = runScenario {
        reporter.onShown(
            ShippingAddressElementAnalyticsData(country = "US")
        )

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.shown")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params).containsEntry(
            "address_data_blob",
            mapOf("address_country_code" to "US"),
        )
    }

    @Test
    fun `onSaveStarted reports address analytics parameters`() = runScenario {
        reporter.onSaveStarted(
            ShippingAddressElementAnalyticsData(
                country = "US",
                autocompleteResultSelected = true,
                editDistance = 3,
            )
        )

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_started")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to true,
                "edit_distance" to 3,
            )
        )
    }

    @Test
    fun `onSaveFailed reports standard error parameters`() = runScenario {
        reporter.onSaveFailed(
            addressData = ShippingAddressElementAnalyticsData(country = "US"),
            error = IllegalStateException("sensitive details"),
        )

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_failed")
        assertThat(params).containsKey("analytics_value")
        assertThat(params).doesNotContainKey("error_message")
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val executor = FakeAnalyticsRequestExecutor()
        val reporter = DefaultShippingAddressElementEventReporter(
            analyticsRequestExecutor = executor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null },
            ),
            checkoutSessionId = "cs_test_123",
        )

        Scenario(reporter, executor).block()
    }

    private data class Scenario(
        val reporter: ShippingAddressElementEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
    )
}
