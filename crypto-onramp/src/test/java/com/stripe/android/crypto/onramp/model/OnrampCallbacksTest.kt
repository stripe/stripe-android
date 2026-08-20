package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnrampCallbacksTest {
    @Test
    fun `additional KYC callback is optional`() = runScenario {
        assertThat(callbacks.additionalKycCallback).isNull()
    }

    @Test
    fun `additional KYC callback is retained`() {
        val callback = OnrampAdditionalKycCallback {}

        runScenario(additionalKycCallback = callback) {
            assertThat(callbacks.additionalKycCallback).isSameInstanceAs(callback)
        }
    }

    private fun runScenario(
        additionalKycCallback: OnrampAdditionalKycCallback? = null,
        block: Scenario.() -> Unit,
    ) {
        val callbacks = OnrampCallbacks()
            .verifyIdentityCallback {}
            .verifyKycCallback {}
            .collectPaymentCallback {}
            .authorizeCallback {}
            .checkoutCallback {}
            .onrampSessionClientSecretProvider { "secret_123" }

        additionalKycCallback?.let(callbacks::additionalKycCallback)

        Scenario(callbacks = callbacks.build()).block()
    }

    private data class Scenario(
        val callbacks: OnrampCallbacks.State,
    )
}
