package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
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
    fun `standalone onSaveCompleted derives analytics from the saved and selected addresses`() = runScenario {
        standaloneReporter.onSaveCompleted(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = createAddressDetails(line1 = "511 Townsend St"),
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
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
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
            addressDetails = createAddressDetails(country = null),
            autocompleteAddressDetails = createAddressDetails(line1 = "511 Townsend St"),
        )

        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `standalone ignores Checkout lifecycle events`() = runScenario {
        standaloneReporter.onSaveStarted(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
        )
        standaloneReporter.onSaveFailed(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
            error = IllegalStateException("sensitive details"),
        )

        addressLauncherEventReporter.showCalls.expectNoEvents()
        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `standalone ignores Checkout cancellation`() = runScenario {
        standaloneReporter.onCanceled(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
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
    fun `Checkout onSaveStarted derives analytics from the saved and selected addresses`() = runScenario {
        checkoutShippingReporter.onSaveStarted(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = createAddressDetails(line1 = "511 Townsend St"),
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_started")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to true,
                "edit_distance" to 1,
            )
        )
    }

    @Test
    fun `Checkout onSaveCompleted omits distance without a selected address`() = runScenario {
        checkoutShippingReporter.onSaveCompleted(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_completed")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
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
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.canceled")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to false,
            )
        )
    }

    @Test
    fun `Checkout onSaveFailed reports standard error parameters`() = runScenario {
        checkoutShippingReporter.onSaveFailed(
            addressDetails = createAddressDetails(),
            autocompleteAddressDetails = null,
            error = IllegalStateException("sensitive details"),
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_failed")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "US",
                "auto_complete_result_selected" to false,
            )
        )
        assertThat(params).containsEntry("analytics_value", "java.lang.IllegalStateException")
        assertThat(params).doesNotContainKey("error_message")
    }

    private fun createAddressDetails(
        country: String? = "US",
        line1: String = "510 Townsend St",
    ) = AddressDetails(
        address = PaymentSheet.Address(
            city = "San Francisco",
            country = country,
            line1 = line1,
            postalCode = "94103",
            state = "CA",
        ),
    )

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
