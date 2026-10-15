package com.stripe.android.checkout.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.networking.AnalyticsEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsModuleTest {
    @Test
    fun `request factory uses checkout elements product usage and configured publishable key`() = runTest {
        val publishableKey = "pk_test_checkout_analytics_module"
        val apiConfiguration = ApiConfiguration(publishableKey).build()
        val providerCalls = Turbine<Unit>()
        val factory = CheckoutAnalyticsModule.provideAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            apiConfigurationProvider = Provider {
                providerCalls.add(Unit)
                apiConfiguration
            },
        )
        providerCalls.expectNoEvents()

        val request = factory.createRequest(
            event = object : AnalyticsEvent {
                override val eventName = "checkout_elements.confirm.started"
            },
            additionalParams = emptyMap(),
        )
        providerCalls.awaitItem()
        providerCalls.expectNoEvents()
        providerCalls.ensureAllEventsConsumed()

        assertThat(request.params["event"]).isEqualTo("checkout_elements.confirm.started")
        assertThat(request.params["product_usage"]).isEqualTo("checkout_elements")
        assertThat(request.params["publishable_key"]).isEqualTo(publishableKey)
    }
}
