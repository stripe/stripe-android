package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementEventReporterTest {
    @Test
    fun `standalone onShown reports the form country`() = runScenario {
        standaloneReporter.onShown(country = "CA")

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEqualTo("CA")
    }

    @Test
    fun `standalone onShown reports an empty country when the form country is unavailable`() = runScenario {
        standaloneReporter.onShown(country = null)

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEmpty()
    }

    @Test
    fun `standalone onSaveCompleted forwards computed values`() = runScenario {
        standaloneReporter.onSaveCompleted(
            country = "US",
            autocompleteResultSelected = true,
            editDistance = 1,
        )

        assertThat(addressLauncherEventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = true,
                editDistance = 1,
            )
        )
    }

    @Test
    fun `standalone onSaveCompleted forwards a missing selection`() = runScenario {
        standaloneReporter.onSaveCompleted(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
        )

        assertThat(addressLauncherEventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
    }

    @Test
    fun `standalone onSaveCompleted does not report without a country`() = runScenario {
        standaloneReporter.onSaveCompleted(
            country = null,
            autocompleteResultSelected = true,
            editDistance = 1,
        )

        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `standalone ignores Checkout lifecycle events`() = runScenario {
        standaloneReporter.onCanceled("US", autocompleteResultSelected = false, editDistance = null)
        standaloneReporter.onSaveStarted("US", autocompleteResultSelected = false, editDistance = null)
        standaloneReporter.onSaveFailed(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
            error = IllegalStateException("sensitive details"),
        )

        addressLauncherEventReporter.showCalls.expectNoEvents()
        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `Checkout onShown reports the current country and session`() = runScenario {
        checkoutShippingReporter.onShown(country = "CA")

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.shown")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf("address_country_code" to "CA")
        )
    }

    @Test
    fun `Checkout onSaveStarted forwards computed analytics values`() = runScenario {
        checkoutShippingReporter.onSaveStarted(
            country = "US",
            autocompleteResultSelected = true,
            editDistance = 1,
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_started")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to true,
                "edit_distance" to 1,
            )
        )
    }

    @Test
    fun `Checkout onSaveCompleted omits distance when no selection was computed`() = runScenario {
        checkoutShippingReporter.onSaveCompleted(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_completed")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to false,
            )
        )
    }

    @Test
    fun `Checkout onCanceled reports canceled`() = runScenario {
        checkoutShippingReporter.onCanceled(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.canceled")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
    }

    @Test
    fun `Checkout onSaveFailed reports standard error parameters`() = runScenario {
        checkoutShippingReporter.onSaveFailed(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
            error = IllegalStateException("sensitive details"),
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_failed")
        assertThat(params).containsKey("analytics_value")
        assertThat(params).doesNotContainKey("error_message")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val addressLauncherEventReporter = FakeAddressLauncherEventReporter()
        Scenario(
            standaloneReporter = StandaloneAddressElementEventReporter(addressLauncherEventReporter),
            checkoutShippingReporter = CheckoutShippingAddressElementEventReporter(
                analyticsRequestExecutor = analyticsRequestExecutor,
                analyticsRequestFactory = AnalyticsRequestFactory(
                    packageManager = null,
                    packageInfo = null,
                    packageName = "",
                    publishableKeyProvider = { "" },
                    networkTypeProvider = { "" },
                    pluginTypeProvider = { null },
                ),
                checkoutSessionId = "cs_test_123",
            ),
            analyticsRequestExecutor = analyticsRequestExecutor,
            addressLauncherEventReporter = addressLauncherEventReporter,
        ).block()

        addressLauncherEventReporter.validate()
    }

    private data class Scenario(
        val standaloneReporter: AddressElementEventReporter,
        val checkoutShippingReporter: AddressElementEventReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val addressLauncherEventReporter: FakeAddressLauncherEventReporter,
    )
}
