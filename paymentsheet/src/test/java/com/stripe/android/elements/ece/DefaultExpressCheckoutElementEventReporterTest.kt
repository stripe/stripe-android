@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import org.junit.Test

internal class DefaultExpressCheckoutElementEventReporterTest {
    @Test
    fun `onEceDisplayed fires event with default params`() = runScenario {
        reporter.onEceDisplayed()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_init")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("ordered_lpms", "link,google_pay")
        assertThat(loggedParams).containsEntry(
            "ece_config",
            mapOf(
                "link_visibility" to "auto",
                "google_pay_visibility" to true,
            ),
        )
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(DurationProvider.Key.ExpressCheckoutElement, true)
            )
        ).isTrue()
    }

    @Test
    fun `onEceDisplayed reports google pay as not visible when disabled`() = runScenario(
        expressCheckoutElementConfiguration = ExpressCheckoutElement.Configuration()
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
            ),
    ) {
        reporter.onEceDisplayed()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry(
            "ece_config",
            mapOf(
                "link_visibility" to "auto",
                "google_pay_visibility" to false,
            ),
        )
    }

    @Test
    fun `onEceWalletTapped fires expected event for Link`() = runScenario {
        val linkButton = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(null)
        )

        reporter.onEceWalletTapped(linkButton)

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_wallet_tapped")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("selected_lpm", "link")
        assertThat(loggedParams).containsKey("link_context")
    }

    @Test
    fun `onEceWalletTapped fires expected event for GooglePay`() = runScenario {
        reporter.onEceWalletTapped(googlePayButton)

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_wallet_tapped")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("selected_lpm", "google_pay")
        assertThat(loggedParams).doesNotContainKey("link_context")
    }

    @Test
    fun `onEcePaymentSuccess fires expected event with Link params`() = runScenario {
        val linkButton = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(null)
        )

        reporter.onEcePaymentSuccess(linkButton)

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_payment_success")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("selected_lpm", "link")
        assertThat(loggedParams).containsKey("link_context")
    }

    @Test
    fun `onEcePaymentFailure fires expected event with error params`() = runScenario {
        reporter.onEcePaymentFailure(
            expressButton = googlePayButton,
            error = ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Payment failed"),
                message = "Payment failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(10),
            ),
        )

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "mc_ece_payment_failure")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("duration", 1.0f)
        assertThat(loggedParams).containsEntry("selected_lpm", "google_pay")
        assertThat(loggedParams).containsEntry("error_message", "googlePay_10")
        assertThat(loggedParams).containsEntry("error_code", "10")
    }

    private class Scenario(
        val reporter: ExpressCheckoutElementEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val googlePayButton: ExpressButton.GooglePay,
    )

    private fun runScenario(
        expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration =
            ExpressCheckoutElement.Configuration(),
        block: Scenario.() -> Unit,
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            availableWallets = listOf(WalletType.GooglePay, WalletType.Link),
            analyticsMetadata = AnalyticsMetadata(
                mapOf("example_analytics_metadata" to AnalyticsMetadata.Value.SimpleBoolean(true))
            ),
        )
        val googlePayConfiguration =
            ExpressCheckoutElement.Configuration.GooglePayConfiguration().build()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = SavedStateHandle(),
            availableExpressButtonTypesFactory = FakeAvailableExpressButtonTypesFactory(
                availableExpressButtonTypes = listOf(
                    ExpressButtonType.Link,
                    ExpressButtonType.GooglePay(googlePayConfiguration),
                ),
            ),
        )
        stateHolder.state = CheckoutControllerStateFactory.create(
            configuration = CheckoutController.Configuration()
                .googlePayConfiguration(GooglePayConfiguration(GooglePayConfiguration.Environment.Test))
                .expressCheckoutElement(expressCheckoutElementConfiguration)
                .build(),
            paymentMethodMetadata = paymentMethodMetadata,
        )
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
            durationProvider = durationProvider,
            stateHolder = stateHolder,
        )

        block(
            Scenario(
                reporter = reporter,
                executor = analyticsRequestExecutor,
                durationProvider = durationProvider,
                paymentMethodMetadata = paymentMethodMetadata,
                googlePayButton = ExpressButton.GooglePay.create(
                    paymentMethodMetadata = paymentMethodMetadata,
                    googlePayConfiguration = googlePayConfiguration,
                    shippingAddressRequired = false,
                ),
            )
        )
    }
}
