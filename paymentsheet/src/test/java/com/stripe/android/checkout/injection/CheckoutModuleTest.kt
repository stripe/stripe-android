package com.stripe.android.checkout.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutModuleTest {
    @Test
    fun `reporter uses checkout usage and reads current API configuration per request`() = runTest {
        var publishableKey = "pk_test_checkout_first"
        val apiConfigurationProviderCalls = Turbine<Unit>()
        val apiConfigurationProvider = Provider {
            apiConfigurationProviderCalls.add(Unit)
            ApiConfiguration(publishableKey).build()
        }
        val metadataProviderCalls = Turbine<Unit>()
        val metadataProvider = Provider<PaymentMethodMetadata?> {
            metadataProviderCalls.add(Unit)
            null
        }
        val executor = FakeAnalyticsRequestExecutor()

        val reporter = CheckoutModule.provideCheckoutEventReporter(
            context = ApplicationProvider.getApplicationContext<Context>(),
            apiConfigurationProvider = apiConfigurationProvider,
            analyticsRequestExecutor = executor,
            durationProvider = FakeDurationProvider(),
            paymentMethodMetadataProvider = metadataProvider,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        apiConfigurationProviderCalls.expectNoEvents()
        metadataProviderCalls.expectNoEvents()

        reporter.succeeded(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = null,
            intentId = null,
        )
        testScheduler.runCurrent()
        apiConfigurationProviderCalls.awaitItem()
        metadataProviderCalls.awaitItem()
        apiConfigurationProviderCalls.expectNoEvents()
        metadataProviderCalls.expectNoEvents()

        publishableKey = "pk_test_checkout_second"
        reporter.succeeded(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = null,
            intentId = null,
        )
        testScheduler.runCurrent()
        apiConfigurationProviderCalls.awaitItem()
        metadataProviderCalls.awaitItem()
        apiConfigurationProviderCalls.ensureAllEventsConsumed()
        metadataProviderCalls.ensureAllEventsConsumed()

        val requests = executor.getExecutedRequests()
        assertThat(requests).hasSize(2)
        assertThat(requests[0].params["event"]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(requests[0].params["product_usage"]).isEqualTo("checkout_elements")
        assertThat(requests[0].params["publishable_key"]).isEqualTo("pk_test_checkout_first")
        assertThat(requests[1].params["product_usage"]).isEqualTo("checkout_elements")
        assertThat(requests[1].params["publishable_key"]).isEqualTo("pk_test_checkout_second")
    }
}
