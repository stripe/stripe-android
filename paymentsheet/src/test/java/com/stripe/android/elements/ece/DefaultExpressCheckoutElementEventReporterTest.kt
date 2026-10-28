@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultExpressCheckoutElementEventReporterTest {
    @Test
    fun `onEceDisplayed fires event with default params`() = runScenario {
        reporter.onEceDisplayed()

        val loggedParams = executor.getExecutedRequests().single().params
        assertThat(loggedParams).containsEntry("event", "elements.express_checkout_element.init")
        assertThat(loggedParams).containsEntry("product_usage", "checkout_elements")
        assertThat(loggedParams).containsEntry("example_analytics_metadata", true)
        assertThat(loggedParams).containsEntry("ordered_lpms", "link,google_pay")
        assertThat(loggedParams).containsEntry(
            "ece_config",
            mapOf(
                "link_visibility" to "automatic",
                "google_pay_visibility" to "automatic",
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
                "link_visibility" to "automatic",
                "google_pay_visibility" to "never",
            ),
        )
    }

    private class Scenario(
        val reporter: ExpressCheckoutElementEventReporter,
        val executor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
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
        val googlePayConfiguration = GooglePayConfiguration().build()
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
                .expressCheckoutElement(expressCheckoutElementConfiguration)
                .build(),
            expressCheckoutElementPaymentMethodMetadata = paymentMethodMetadata,
        )
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            defaultProductUsageTokens = setOf("checkout_elements"),
            apiConfigurationProvider = { PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG },
        )
        val reporter = DefaultExpressCheckoutElementEventReporter(
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            stateHolder = stateHolder,
        )

        block(
            Scenario(
                reporter = reporter,
                executor = analyticsRequestExecutor,
                durationProvider = durationProvider,
            )
        )
    }
}
