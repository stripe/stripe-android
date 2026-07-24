package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Test

internal class DefaultExpressCheckoutElementEventReporterTest {
    @Test
    fun `onEceDisplayed fires expected event`() = runScenario {
        reporter.onEceDisplayed()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_init")
    }

    @Test
    fun `onEceWalletTapped fires expected event`() = runScenario {
        reporter.onEceWalletTapped()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_wallet_tapped")
    }

    @Test
    fun `onEcePaymentSuccess fires expected event`() = runScenario {
        reporter.onEcePaymentSuccess()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_payment_success")
    }

    @Test
    fun `onEcePaymentFailure fires expected event`() = runScenario {
        reporter.onEcePaymentFailure()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_payment_failure")
    }

    private class Scenario(
        val reporter: ExpressCheckoutElementEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
    )

    private fun runScenario(
        block: Scenario.() -> Unit,
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val reporter = DefaultExpressCheckoutElementEventReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "",
                publishableKeyProvider = { "" },
                networkTypeProvider = { "" },
                pluginTypeProvider = { null },
            ),
        )

        block(
            Scenario(
                reporter = reporter,
                executor = analyticsRequestExecutor,
            )
        )
    }
}
