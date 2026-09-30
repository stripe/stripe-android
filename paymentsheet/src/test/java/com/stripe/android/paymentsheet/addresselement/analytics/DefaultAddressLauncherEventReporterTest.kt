package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultAddressLauncherEventReporterTest {
    @Test
    fun `onShow fires analytics event with address country`() = runScenario {
        reporter.onShow(country = "CA")

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "mc_address_show")
        assertThat(params["address_data_blob"]).isEqualTo(mapOf("address_country_code" to "CA"))
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(DurationProvider.Key.AddressElementCompletion, reset = true)
            )
        ).isTrue()
    }

    @Test
    fun `onCompleted includes the picked result analytics fields`() = runScenario {
        reporter.onCompleted(
            country = "CA",
            autocompleteResultSelected = true,
            editDistance = 2,
        )

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "mc_address_completed")
        assertThat(params["address_data_blob"]).isEqualTo(
            mapOf(
                "address_country_code" to "CA",
                "auto_complete_result_selected" to true,
                "edit_distance" to 2,
            )
        )
        assertThat(params).containsEntry("ms_to_complete", 1000.0)
    }

    @Test
    fun `onCompleted omits edit distance when no result was picked`() = runScenario {
        reporter.onCompleted(
            country = "US",
            autocompleteResultSelected = false,
            editDistance = null,
        )

        val params = executor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "mc_address_completed")
        val addressData = params["address_data_blob"] as Map<*, *>
        assertThat(addressData).containsEntry("address_country_code", "US")
        assertThat(addressData).containsEntry("auto_complete_result_selected", false)
        assertThat(addressData).doesNotContainKey("edit_distance")
    }

    private fun runScenario(block: Scenario.() -> Unit) = runTest {
        val executor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider()
        val reporter = DefaultAddressLauncherEventReporter(
            analyticsRequestExecutor = executor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null },
            ),
            durationProvider = durationProvider,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        Scenario(reporter, executor, durationProvider).block()
    }

    private data class Scenario(
        val reporter: DefaultAddressLauncherEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
    )
}
