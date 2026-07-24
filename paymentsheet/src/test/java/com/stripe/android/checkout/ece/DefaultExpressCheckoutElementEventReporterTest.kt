@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
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
    fun `onEceWalletTapped fires expected event for Link`() = runScenario {
        val linkButton = ExpressButton.Link.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkAccountInfo = LinkAccountUpdate.Value(null)
        )
        reporter.onEceWalletTapped(linkButton)

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_wallet_tapped")
        assertThat(loggedParams).containsEntry("selected_lpm", "link")
        assertThat(loggedParams).containsKey("link_context")
    }

    @Test
    fun `onEceWalletTapped fires expected event for GooglePay`() = runScenario {
        val googlePayConfiguration = GooglePayConfiguration(environment = GooglePayConfiguration.Environment.Test).build()
        val googlePayButton = ExpressButton.GooglePay.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            googlePayConfiguration = googlePayConfiguration,
        )
        reporter.onEceWalletTapped(googlePayButton)

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_wallet_tapped")
        assertThat(loggedParams).containsEntry("selected_lpm", "google_pay")
        assertThat(loggedParams).doesNotContainKey("link_context")
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
