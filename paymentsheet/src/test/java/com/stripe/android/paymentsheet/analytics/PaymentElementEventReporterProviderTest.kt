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
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.embedded.EmbeddedActivityModule
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@RunWith(RobolectricTestRunner::class)
class PaymentElementEventReporterProviderTest {

    @Test
    fun `Checkout reporter uses PaymentElement and resolves API configuration lazily`() = runScenario {
        apiConfigurationProvider.getCalls.expectNoEvents()

        eventReporter.onAnalyticsEvent(TestEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
        assertThat(request.params).containsEntry("publishable_key", DEFAULT_API_CONFIG.publishableKey)
        apiConfigurationProvider.getCalls.awaitItem()
    }

    @Test
    fun `Embedded reporter uses PaymentElement for CheckoutSession metadata`() = runScenario(
        useEmbeddedProvider = true,
        paymentMethodMetadata = checkoutPaymentMethodMetadata("cs_test_embedded"),
    ) {
        eventReporter.onAnalyticsEvent(TestEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("product_usage", "PaymentElement")
    }

    @Test
    fun `Embedded reporter preserves existing L3 product usage outside CheckoutSession`() = runScenario(
        useEmbeddedProvider = true,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ) {
        eventReporter.onAnalyticsEvent(TestEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("product_usage", "EmbeddedPaymentElement")
    }

    private fun runScenario(
        useEmbeddedProvider: Boolean = false,
        paymentMethodMetadata: PaymentMethodMetadata? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val analyticsRequestV2Executor = FakeAnalyticsRequestV2Executor()
        val durationProvider = FakeDurationProvider()
        val logger = FakeUserFacingLogger()
        val workContext = UnconfinedTestDispatcher(testScheduler)
        val apiConfigurationProvider = FakeApiConfigurationProvider(DEFAULT_API_CONFIG)
        val eventReporter = if (useEmbeddedProvider) {
            EmbeddedActivityModule.provideEventReporter(
                context = context,
                mode = EventReporter.Mode.Embedded,
                analyticsRequestExecutor = analyticsRequestExecutor,
                analyticsRequestV2Executor = analyticsRequestV2Executor,
                durationProvider = durationProvider,
                analyticEventCallbackProvider = { null },
                workContext = workContext,
                logger = logger,
                paymentMethodMetadata = requireNotNull(paymentMethodMetadata),
                paymentMethodMetadataProvider = { paymentMethodMetadata },
                productUsage = setOf("EmbeddedPaymentElement"),
                apiConfigurationProvider = apiConfigurationProvider,
            )
        } else {
            CheckoutModule.provideEventReporter(
                context = context,
                mode = EventReporter.Mode.Embedded,
                analyticsRequestExecutor = analyticsRequestExecutor,
                analyticsRequestV2Executor = analyticsRequestV2Executor,
                durationProvider = durationProvider,
                analyticEventCallbackProvider = { null },
                workContext = workContext,
                logger = logger,
                paymentMethodMetadataProvider = { paymentMethodMetadata },
                apiConfigurationProvider = apiConfigurationProvider,
            )
        }

        Scenario(
            eventReporter = eventReporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            apiConfigurationProvider = apiConfigurationProvider,
        ).block()

        analyticsRequestExecutor.validate()
        analyticsRequestV2Executor.validate()
        apiConfigurationProvider.ensureAllEventsConsumed()
        assertThat(logger.getLoggedMessages()).isEmpty()
    }

    private data class Scenario(
        val eventReporter: DefaultEventReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val apiConfigurationProvider: FakeApiConfigurationProvider,
    )

    private fun checkoutPaymentMethodMetadata(sessionId: String): PaymentMethodMetadata {
        val response = CheckoutSessionResponseFactory.create(id = sessionId)
        return PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = response.id,
                instancesKey = "PaymentElementEventReporterProviderTest",
                checkoutSessionResponse = response,
                collectedEmail = null,
            ),
        )
    }

    private object TestEvent : AnalyticsEvent {
        override val eventName: String = "payment_element_event_reporter_provider_test"
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
