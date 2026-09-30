package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementEventReporterTest {
    @Test
    fun `onShown reports the form country`() = runScenario {
        reporter.onShown(country = "CA")

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEqualTo("CA")
    }

    @Test
    fun `onShown reports an empty country when the form country is unavailable`() = runScenario {
        reporter.onShown(country = null)

        assertThat(addressLauncherEventReporter.showCalls.awaitItem()).isEmpty()
    }

    @Test
    fun `onSaveCompleted forwards computed values`() = runScenario {
        reporter.onSaveCompleted(
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
    fun `onSaveCompleted does not report when the saved address has no country`() = runScenario {
        reporter.onSaveCompleted(
            country = null,
            autocompleteResultSelected = true,
            editDistance = 1,
        )

        addressLauncherEventReporter.completedCalls.expectNoEvents()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val addressLauncherEventReporter = FakeAddressLauncherEventReporter()

        Scenario(
            reporter = StandaloneAddressElementEventReporter(addressLauncherEventReporter),
            addressLauncherEventReporter = addressLauncherEventReporter,
        ).block()

        addressLauncherEventReporter.validate()
    }

    private data class Scenario(
        val reporter: AddressElementEventReporter,
        val addressLauncherEventReporter: FakeAddressLauncherEventReporter,
    )
}
