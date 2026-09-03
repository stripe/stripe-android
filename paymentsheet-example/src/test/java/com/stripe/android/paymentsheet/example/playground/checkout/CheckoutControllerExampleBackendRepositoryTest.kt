package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class CheckoutControllerExampleBackendRepositoryTest {
    @Test
    fun `checkout session URL uses default backend URL`() {
        val result = checkoutSessionUrl(
            defaultBackendUrl = "https://default.example/",
            customBackendUrl = null,
            endpoint = "checkout_session",
        )

        assertThat(result).isEqualTo("https://default.example/checkout_session")
    }

    @Test
    fun `checkout session URL uses custom backend URL`() {
        val result = checkoutSessionUrl(
            defaultBackendUrl = "https://default.example/",
            customBackendUrl = "https://custom.example/",
            endpoint = "checkout_session",
        )

        assertThat(result).isEqualTo("https://custom.example/checkout_session")
    }

    @Test
    fun `backend error message is preserved`() {
        val result = parseCheckoutSessionError(
            json = Json,
            errorData = """{"error":"Invalid customer"}""".encodeToByteArray(),
        )

        assertThat(result).isEqualTo("Invalid customer")
    }

    @Test
    fun `malformed backend error has no message`() {
        val result = parseCheckoutSessionError(
            json = Json,
            errorData = "Not JSON".encodeToByteArray(),
        )

        assertThat(result).isNull()
    }
}
