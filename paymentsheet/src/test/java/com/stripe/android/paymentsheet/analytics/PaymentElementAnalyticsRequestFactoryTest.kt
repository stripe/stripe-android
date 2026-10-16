package com.stripe.android.paymentsheet.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.CheckoutModule
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.embedded.EmbeddedActivityModule
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class PaymentElementAnalyticsRequestFactoryTest {

    @Test
    fun `Checkout factory defaults UI events to PaymentElement and resolves API configuration lazily`() = runTest {
        val apiConfigurationProvider = FakeApiConfigurationProvider(DEFAULT_API_CONFIG)
        val factory = CheckoutModule.provideEventReporterAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            apiConfigurationProvider = apiConfigurationProvider,
        )

        apiConfigurationProvider.getCalls.expectNoEvents()

        val request = createRequest(factory)

        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
        assertThat(request.params).containsEntry("publishable_key", DEFAULT_API_CONFIG.publishableKey)
        apiConfigurationProvider.getCalls.awaitItem()
        apiConfigurationProvider.ensureAllEventsConsumed()
    }

    @Test
    fun `Embedded factory uses PaymentElement for CheckoutSession metadata`() = runTest {
        val apiConfigurationProvider = FakeApiConfigurationProvider(DEFAULT_API_CONFIG)
        val factory = EmbeddedActivityModule.provideEventReporterAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            paymentMethodMetadata = checkoutPaymentMethodMetadata("cs_test_embedded"),
            productUsage = setOf("EmbeddedPaymentElement"),
            apiConfigurationProvider = apiConfigurationProvider,
        )

        val request = createRequest(factory)

        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
        apiConfigurationProvider.getCalls.awaitItem()
        apiConfigurationProvider.ensureAllEventsConsumed()
    }

    @Test
    fun `Embedded factory preserves existing L3 product usage outside CheckoutSession`() = runTest {
        val apiConfigurationProvider = FakeApiConfigurationProvider(DEFAULT_API_CONFIG)
        val factory = EmbeddedActivityModule.provideEventReporterAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            productUsage = setOf("EmbeddedPaymentElement"),
            apiConfigurationProvider = apiConfigurationProvider,
        )

        val request = createRequest(factory)

        assertThat(request.params).containsEntry("product_usage", "EmbeddedPaymentElement")
        apiConfigurationProvider.getCalls.awaitItem()
        apiConfigurationProvider.ensureAllEventsConsumed()
    }

    private fun createRequest(factory: PaymentAnalyticsRequestFactory) = factory.createRequest(
        event = TestEvent,
        additionalParams = emptyMap(),
        publishableKeyOverride = null,
    )

    private fun checkoutPaymentMethodMetadata(sessionId: String): PaymentMethodMetadata {
        val response = CheckoutSessionResponseFactory.create(id = sessionId)
        return PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = response.id,
                instancesKey = "PaymentElementAnalyticsRequestFactoryTest",
                checkoutSessionResponse = response,
                collectedEmail = null,
            ),
        )
    }

    private object TestEvent : AnalyticsEvent {
        override val eventName: String = "payment_element_analytics_factory_test"
    }

    private class FakeApiConfigurationProvider(
        private val apiConfiguration: ApiConfiguration.State,
    ) : Provider<ApiConfiguration.State> {
        val getCalls = Turbine<Unit>()

        override fun get(): ApiConfiguration.State {
            getCalls.add(Unit)
            return apiConfiguration
        }

        fun ensureAllEventsConsumed() {
            getCalls.ensureAllEventsConsumed()
        }
    }
}
