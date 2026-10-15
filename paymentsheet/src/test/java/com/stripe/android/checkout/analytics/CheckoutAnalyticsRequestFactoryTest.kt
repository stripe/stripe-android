@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.CheckoutModule
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.link.injection.NativeLinkModule
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.embedded.EmbeddedActivityModule
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementViewModelModule
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsRequestFactoryTest {
    @Test
    fun checkoutAnalyticsFactoryAddsCheckoutUsageAndReporterSessionContext() = runTest {
        val executor = FakeAnalyticsRequestExecutor()
        val apiConfigurationCalls = Turbine<Unit>()
        val apiConfigurationProvider = Provider {
            apiConfigurationCalls.add(Unit)
            TEST_API_CONFIGURATION
        }
        val requestFactory = CheckoutAnalyticsModule.provideAnalyticsRequestFactory(
            context = context(),
            apiConfigurationProvider = apiConfigurationProvider,
        )
        val reporter = CheckoutEventReporter(
            analyticsRequestExecutor = executor,
            analyticsRequestFactory = requestFactory,
            timeSource = TestTimeSource(),
        )

        apiConfigurationCalls.expectNoEvents()
        reporter.reportClearPaymentOption(
            CheckoutSessionResponseFactory.create(id = "cs_test_analytics")
        )

        assertThat(apiConfigurationCalls.awaitItem()).isEqualTo(Unit)
        apiConfigurationCalls.ensureAllEventsConsumed()
        val request = executor.getExecutedRequests().single()
        assertThat(request.params).containsEntry("event", "checkout_elements.clear_payment_option")
        assertThat(request.params).containsEntry("product_usage", "checkout_elements")
        assertThat(request.params).containsEntry("checkout_session_id", "cs_test_analytics")
        assertThat(request.params).containsEntry("publishable_key", TEST_API_CONFIGURATION.publishableKey)
    }

    @Test
    fun rootCheckoutFactoryAddsPaymentElementUsage() {
        val requestFactory = CheckoutModule.provideEventReporterAnalyticsRequestFactory(
            context = context(),
            apiConfigurationProvider = Provider { TEST_API_CONFIGURATION },
        )

        val request = requestFactory.createRequest(
            event = testEvent(),
            additionalParams = emptyMap(),
        )

        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
        assertThat(request.params).containsEntry("publishable_key", TEST_API_CONFIGURATION.publishableKey)
    }

    @Test
    fun embeddedActivityCheckoutFactoryUsesPaymentElementUsage() {
        val requestFactory = EmbeddedActivityModule.provideEventReporterAnalyticsRequestFactory(
            context = context(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = checkoutSessionMetadata(),
            ),
            productUsage = setOf("EmbeddedPaymentElement"),
            apiConfigurationProvider = Provider { TEST_API_CONFIGURATION },
        )

        val request = requestFactory.createRequest(
            event = testEvent(),
            additionalParams = emptyMap(),
        )

        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
    }

    @Test
    fun embeddedActivityFactoryPreservesUsageForOtherIntegrations() {
        val requestFactory = EmbeddedActivityModule.provideEventReporterAnalyticsRequestFactory(
            context = context(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.IntentFirst("pi_test_secret"),
            ),
            productUsage = setOf("L3Token"),
            apiConfigurationProvider = Provider { TEST_API_CONFIGURATION },
        )

        val request = requestFactory.createRequest(
            event = testEvent(),
            additionalParams = emptyMap(),
        )

        assertThat(request.params).containsEntry("product_usage", "L3Token")
    }

    @Test
    fun embeddedAndNativeLinkProvidersPassThroughExistingUsage() {
        val requestFactory = PaymentAnalyticsRequestFactory(
            context = context(),
            publishableKey = TEST_API_CONFIGURATION.publishableKey,
            defaultProductUsageTokens = setOf("L3Token"),
        )

        val embeddedFactory = EmbeddedPaymentElementViewModelModule
            .provideEventReporterAnalyticsRequestFactory(requestFactory)
        val nativeLinkFactory = NativeLinkModule
            .provideEventReporterAnalyticsRequestFactory(requestFactory)

        assertThat(embeddedFactory).isSameInstanceAs(requestFactory)
        assertThat(nativeLinkFactory).isSameInstanceAs(requestFactory)
        val request = nativeLinkFactory.createRequest(
            event = testEvent(),
            additionalParams = emptyMap(),
        )
        assertThat(request.params).containsEntry("product_usage", "L3Token")
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun testEvent(): AnalyticsEvent = object : AnalyticsEvent {
        override val eventName: String = "test_analytics_event"
    }

    private fun checkoutSessionMetadata(): IntegrationMetadata.CheckoutSession {
        val response = CheckoutSessionResponseFactory.create(id = "cs_test_activity")
        return IntegrationMetadata.CheckoutSession(
            id = response.id,
            instancesKey = "checkout_instances",
            checkoutSessionResponse = response,
            collectedEmail = null,
        )
    }

    private companion object {
        val TEST_API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = "pk_test_checkout_analytics",
            stripeAccountId = null,
        )
    }
}
