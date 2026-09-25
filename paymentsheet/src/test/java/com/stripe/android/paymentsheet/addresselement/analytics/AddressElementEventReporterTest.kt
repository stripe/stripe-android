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
    fun `standalone onShown reports the initial country`() = runScenario {
        standaloneReporter.onShown(
            snapshot(
                address = addressDetails(country = "CA"),
                initialAddress = addressDetails(country = "US"),
            )
        )

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEqualTo("US")
    }

    @Test
    fun `standalone onShown reports an empty country without an initial address`() = runScenario {
        standaloneReporter.onShown(snapshot(address = addressDetails(country = "US")))

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEmpty()
    }

    @Test
    fun `standalone onSaveCompleted compares the saved address with the initial address`() = runScenario {
        standaloneReporter.onSaveCompleted(
            snapshot(
                address = addressDetails(country = "US", line1 = "510 Townsend Sta"),
                initialAddress = addressDetails(country = "US", line1 = "510 Townsend St"),
            )
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
    fun `standalone onSaveCompleted without an initial address reports no autocomplete selection`() = runScenario {
        standaloneReporter.onSaveCompleted(
            snapshot(address = addressDetails(country = "US", line1 = "510 Townsend St"))
        )

        assertThat(addressLauncherEventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = 17,
            )
        )
    }

    @Test
    fun `standalone onSaveCompleted does not report without a country`() = runScenario {
        standaloneReporter.onSaveCompleted(snapshot(address = AddressDetails()))

        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `standalone does not report canceled or save started and failed events`() = runScenario {
        val snapshot = snapshot(address = addressDetails(country = "US"))

        standaloneReporter.onCanceled(snapshot)
        standaloneReporter.onSaveStarted(snapshot)
        standaloneReporter.onSaveFailed(snapshot, IllegalStateException("save failed"))

        addressLauncherEventReporter.showCalls.expectNoEvents()
        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    @Test
    fun `checkout shipping onShown reports the current country and Checkout Session`() = runScenario {
        checkoutShippingReporter.onShown(
            snapshot(
                address = addressDetails(country = "CA"),
                initialAddress = addressDetails(country = "US"),
            )
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.shown")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(params).containsEntry(
            "address_data_blob",
            mapOf("address_country_code" to "CA"),
        )
    }

    @Test
    fun `checkout shipping onSaveStarted compares the saved address with the autocomplete selection`() =
        runScenario {
            checkoutShippingReporter.onSaveStarted(
                snapshot(
                    address = addressDetails(country = "US", line1 = "510 Townsend Sta"),
                    autocompleteSelectedAddress = addressDetails(country = "US", line1 = "510 Townsend St"),
                )
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
    fun `checkout shipping does not treat the initial address as an autocomplete selection`() = runScenario {
        checkoutShippingReporter.onSaveCompleted(
            snapshot(
                address = addressDetails(country = "US", line1 = "510 Townsend St"),
                initialAddress = addressDetails(country = "US", line1 = "510 Townsend St"),
            )
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
    fun `checkout shipping onCanceled reports canceled`() = runScenario {
        checkoutShippingReporter.onCanceled(snapshot(address = addressDetails(country = "US")))

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.canceled")
        assertThat(params).containsEntry("checkout_session_id", "cs_test_123")
    }

    @Test
    fun `checkout shipping onSaveFailed reports standard error parameters`() = runScenario {
        checkoutShippingReporter.onSaveFailed(
            snapshot = snapshot(address = addressDetails(country = "US")),
            error = IllegalStateException("sensitive details"),
        )

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.shipping_address.save_failed")
        assertThat(params).containsKey("analytics_value")
        assertThat(params).doesNotContainKey("error_message")
    }

    private fun addressDetails(
        country: String,
        line1: String? = null,
    ) = AddressDetails(
        address = PaymentSheet.Address(
            country = country,
            line1 = line1,
        )
    )

    private fun snapshot(
        address: AddressDetails,
        initialAddress: AddressDetails? = null,
        autocompleteSelectedAddress: AddressDetails? = null,
    ) = AddressElementAnalyticsSnapshot(
        address = address,
        initialAddress = initialAddress,
        autocompleteSelectedAddress = autocompleteSelectedAddress,
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
