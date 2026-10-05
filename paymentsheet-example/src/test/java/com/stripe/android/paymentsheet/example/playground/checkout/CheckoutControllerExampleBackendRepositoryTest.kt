package com.stripe.android.paymentsheet.example.playground.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class CheckoutControllerExampleBackendRepositoryTest {
    @Test
    fun `selected merchant is used`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.merchant, Merchant.JP)
        }

        assertThat(settings.snapshot().backendMerchant()).isEqualTo(Merchant.JP)
    }

    @Test
    fun `automatic tax uses tax merchant`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.merchant, Merchant.JP)
            update(session.automaticTax, true)
        }

        assertThat(settings.snapshot().backendMerchant()).isEqualTo(Merchant.US_TAX)
    }

    @Test
    fun `custom merchant takes precedence over automatic tax merchant`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.automaticTax, true)
            update(session.merchant, Merchant.Custom)
            update(session.customSecretKey, "sk_test_custom")
            update(session.customPublishableKey, "pk_test_custom")
        }

        assertThat(settings.snapshot().backendMerchant()).isEqualTo(Merchant.Custom)
    }

    @Test
    fun `custom publishable key bypasses backend lookup`() = runTest {
        val backend = FakePublishableKeyBackend()
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.customPublishableKey, "pk_test_custom")
        }

        assertThat(settings.snapshot().publishableKey(backend)).isEqualTo("pk_test_custom")
        backend.publishableKeyRequests.expectNoEvents()
        backend.ensureAllEventsConsumed()
    }

    @Test
    fun `missing custom publishable key uses backend lookup`() = runTest {
        val backend = FakePublishableKeyBackend()
        val settings = CheckoutPlaygroundSettings.createInMemory()

        assertThat(settings.snapshot().publishableKey(backend)).isEqualTo("pk_test_backend")
        backend.publishableKeyRequests.awaitItem()
        backend.ensureAllEventsConsumed()
    }

    @Test
    fun `custom API applies and clears API host override`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.customStripeApi, "example-api.tunnel.stripe.me")
        }

        try {
            settings.snapshot().applyCustomStripeApi()
            assertThat(ApiRequest.API_HOST_OVERRIDE).isEqualTo("https://example-api.tunnel.stripe.me")

            settings.update(session.customStripeApi, null)
            settings.snapshot().applyCustomStripeApi()
            assertThat(ApiRequest.API_HOST_OVERRIDE).isNull()
        } finally {
            ApiRequest.API_HOST_OVERRIDE = null
        }
    }
}

private class FakePublishableKeyBackend : CheckoutPlaygroundBackend {
    val publishableKeyRequests = Turbine<Unit>()

    override suspend fun fetchPublishableKey(): String {
        publishableKeyRequests.add(Unit)
        return "pk_test_backend"
    }

    override suspend fun createCustomer(requestParams: JsonObject): String = error("Unexpected call")

    override suspend fun attachPaymentMethod(paymentMethodId: String, customerId: String) = error("Unexpected call")

    override suspend fun createCheckoutSession(requestParams: JsonObject): String = error("Unexpected call")

    fun ensureAllEventsConsumed() {
        publishableKeyRequests.ensureAllEventsConsumed()
    }
}
