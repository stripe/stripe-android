package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnrampCallbacksTest {
    @Test
    fun `KYC callback is optional`() = runScenario {
        assertThat(callbacks.kycCallback).isNull()
    }

    @Test
    fun `KYC callback is retained`() {
        val callback = OnrampKycCallback {}

        runScenario(kycCallback = callback) {
            assertThat(callbacks.kycCallback).isSameInstanceAs(callback)
        }
    }

    private fun runScenario(
        kycCallback: OnrampKycCallback? = null,
        block: Scenario.() -> Unit,
    ) {
        val callbacks = OnrampCallbacks()
            .verifyIdentityCallback {}
            .verifyKycCallback {}
            .collectPaymentCallback {}
            .authorizeCallback {}
            .checkoutCallback {}
            .onrampSessionClientSecretProvider { "secret_123" }

        kycCallback?.let(callbacks::kycCallback)

        Scenario(callbacks = callbacks.build()).block()
    }

    private data class Scenario(
        val callbacks: OnrampCallbacks.State,
    )
}
